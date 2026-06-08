package com.mango.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.exception.MangaDexException;
import com.mango.exception.RegraNegocioException;
import com.mango.model.Capitulo;
import com.mango.model.FiltroBusca;
import com.mango.model.Manga;
import com.mango.model.ResultadoBusca;
import com.mango.repository.CacheBuscaRepository;
import com.mango.util.AppConfig;
import com.mango.util.HttpJsonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço de catálogo do UC1 — orquestra a API do MangaDex, o cache local e o
 * mapeamento de JSON para o domínio.
 *
 * <p>Regras de negócio implementadas: RN1.1 (termo mínimo), RN1.2 (cache 30 min),
 * RN1.3 (paginação de 20), RN1.4/1.5 (delegadas ao {@link HttpJsonClient}),
 * RN1.6 (idioma preferido pt-br com fallback en, com deduplicação de capítulos).
 * Os fluxos EX1/EX2 degradam para o cache expirado quando a API está fora.</p>
 */
public final class MangaDexService {

    private static final Logger log = LoggerFactory.getLogger(MangaDexService.class);

    /** Limite prático de offset da API do MangaDex. */
    private static final int OFFSET_MAXIMO = 10_000;
    private static final int LIMITE_FEED = 100;

    private final HttpJsonClient http;
    private final CacheBuscaRepository cache;
    private final ObjectMapper mapper = new ObjectMapper();

    public MangaDexService() {
        this(new HttpJsonClient(), new CacheBuscaRepository());
        cache.removerExpirados();   // higiene do cache no start (não roda em testes com mock)
    }

    /** Construtor para injeção de dependências (facilita testes com Mockito). */
    public MangaDexService(final HttpJsonClient http, final CacheBuscaRepository cache) {
        this.http = http;
        this.cache = cache;
    }

    // ------------------------------------------------------------------ busca

    /**
     * Busca mangás no catálogo conforme os filtros (fluxo principal do UC1).
     *
     * <p>Se a API falhar (EX1/EX2) e houver um resultado em cache — mesmo expirado —
     * ele é servido como degradação graciosa, marcado como {@code doCache}.</p>
     *
     * @throws RegraNegocioException se o termo violar a RN1.1
     * @throws MangaDexException     em falha de comunicação sem cache disponível
     */
    public ResultadoBusca buscar(final FiltroBusca filtro) {
        validarTermo(filtro.termo());

        final String chave = filtro.chaveCache();
        final Optional<String> emCache = cache.buscar(chave);          // RN1.2 / FA2
        if (emCache.isPresent()) {
            try {
                log.debug("Resultado de busca servido do cache para '{}'", filtro.termo());
                return parseResultado(mapper.readTree(emCache.get()), filtro.pagina(), true);
            } catch (final Exception e) {
                log.warn("Cache corrompido para a chave {}; refazendo a busca.", chave);
            }
        }

        final String url = montarUrlBusca(filtro);
        try {
            final JsonNode root = http.getJson(url);
            cache.salvar(chave, root.toString());
            return parseResultado(root, filtro.pagina(), false);
        } catch (final MangaDexException e) {
            // EX1/EX2: tenta o cache expirado antes de propagar o erro.
            final Optional<String> stale = cache.buscarIgnorandoValidade(chave);
            if (stale.isPresent()) {
                try {
                    log.warn("API indisponível; servindo cache antigo. Causa: {}", e.getMessage());
                    return parseResultado(mapper.readTree(stale.get()), filtro.pagina(), true);
                } catch (final Exception ignored) {
                    // cache ilegível: cai no throw abaixo
                }
            }
            throw e;
        }
    }

    private void validarTermo(final String termo) {
        // FA1: termo vazio é permitido (lista os mangás populares).
        if (!termo.isBlank() && termo.strip().length() < AppConfig.TERMO_BUSCA_MINIMO) {
            throw new RegraNegocioException(
                    "Digite ao menos " + AppConfig.TERMO_BUSCA_MINIMO + " caracteres para buscar.");
        }
    }

    private String montarUrlBusca(final FiltroBusca filtro) {
        final int offset = filtro.pagina() * AppConfig.TAMANHO_PAGINA;       // RN1.3
        final StringBuilder url = new StringBuilder(AppConfig.API_BASE)
                .append("/manga?limit=").append(AppConfig.TAMANHO_PAGINA)
                .append("&offset=").append(offset)
                .append("&includes[]=cover_art&includes[]=author")
                .append("&contentRating[]=safe&contentRating[]=suggestive");

        if (filtro.termo().isBlank()) {
            url.append("&order[followedCount]=desc");                        // FA1: populares
        } else {
            url.append("&title=").append(encode(filtro.termo()))
               .append("&order[relevance]=desc");
        }
        // NOTA (MNG-32): filtros de gênero/autor exigem resolver tag-ids via
        // GET /manga/tag e author-ids via GET /author. Próximo passo do UC1.
        return url.toString();
    }

    // ------------------------------------------------------------------ ficha

    /** Carrega a ficha completa de um mangá (passo 8 do fluxo principal). */
    public Manga detalhar(final String mangaId) {
        final String url = AppConfig.API_BASE + "/manga/" + mangaId
                + "?includes[]=cover_art&includes[]=author";
        final JsonNode root = http.getJson(url);
        final JsonNode data = root.get("data");
        if (data == null || data.isMissingNode()) {
            throw new MangaDexException("Mangá não encontrado: " + mangaId);
        }
        return parseManga(data);
    }

    /**
     * Lista os capítulos de um mangá no idioma preferido com fallback (RN1.6).
     *
     * <p>Pagina o feed completo (acima de 100 capítulos) e deduplica por número:
     * quando o mesmo capítulo existe em pt-br e en (ou em vários grupos), mantém
     * uma única entrada, preferindo pt-br. Oneshots sem número não são colapsados.</p>
     */
    public List<Capitulo> listarCapitulos(final String mangaId) {
        final Map<String, Capitulo> porChave = new LinkedHashMap<>();
        int offset = 0;
        int total = Integer.MAX_VALUE;

        while (offset < total && offset < OFFSET_MAXIMO) {
            final String url = AppConfig.API_BASE + "/manga/" + mangaId + "/feed"
                    + "?limit=" + LIMITE_FEED + "&offset=" + offset
                    + "&order[volume]=asc&order[chapter]=asc"
                    + "&translatedLanguage[]=" + AppConfig.IDIOMA_PREFERIDO
                    + "&translatedLanguage[]=" + AppConfig.IDIOMA_FALLBACK
                    + "&contentRating[]=safe&contentRating[]=suggestive";

            final JsonNode root = http.getJson(url);
            total = root.path("total").asInt(0);
            final JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                break;
            }
            for (final JsonNode node : data) {
                final Capitulo c = parseCapitulo(node, mangaId);
                final String chave = (c.numero() == null || c.numero().isBlank())
                        ? "id:" + c.id()
                        : "num:" + c.numero();
                final Capitulo existente = porChave.get(chave);
                if (existente == null) {
                    porChave.put(chave, c);
                } else if (AppConfig.IDIOMA_PREFERIDO.equals(c.idioma())
                        && !AppConfig.IDIOMA_PREFERIDO.equals(existente.idioma())) {
                    porChave.put(chave, c);   // RN1.6: pt-br tem prioridade sobre o fallback
                }
            }
            offset += LIMITE_FEED;
        }
        return new ArrayList<>(porChave.values());
    }

    // ------------------------------------------------------------ parsing JSON

    private ResultadoBusca parseResultado(final JsonNode root, final int pagina, final boolean doCache) {
        final List<Manga> mangas = new ArrayList<>();
        final JsonNode data = root.get("data");
        if (data != null && data.isArray()) {
            for (final JsonNode node : data) {
                mangas.add(parseManga(node));
            }
        }
        final int total = root.path("total").asInt(mangas.size());
        return new ResultadoBusca(mangas, pagina, total, AppConfig.TAMANHO_PAGINA, doCache);
    }

    private Manga parseManga(final JsonNode node) {
        final String id = node.path("id").asText();
        final JsonNode attr = node.path("attributes");

        final String titulo = escolherLocalizado(attr.path("title"), attr.path("altTitles"), "(sem título)");
        final String sinopse = escolherLocalizado(attr.path("description"), null, "Sem sinopse disponível.");
        final String status = attr.path("status").asText("desconhecido");
        final String idiomaOriginal = attr.path("originalLanguage").asText("");

        final List<String> generos = new ArrayList<>();
        for (final JsonNode tag : attr.path("tags")) {
            if ("genre".equals(tag.path("attributes").path("group").asText())) {
                final String nome = escolherLocalizado(tag.path("attributes").path("name"), null, "");
                if (!nome.isBlank()) {
                    generos.add(nome);
                }
            }
        }

        String capaUrl = null;
        String autor = null;
        for (final JsonNode rel : node.path("relationships")) {
            final String tipo = rel.path("type").asText();
            if ("cover_art".equals(tipo)) {
                final String arquivo = rel.path("attributes").path("fileName").asText("");
                if (!arquivo.isBlank()) {
                    capaUrl = AppConfig.COVER_BASE + "/" + id + "/" + arquivo + ".512.jpg";
                }
            } else if ("author".equals(tipo) && autor == null) {
                final String nome = rel.path("attributes").path("name").asText("");
                autor = nome.isBlank() ? null : nome;
            }
        }

        return new Manga(id, titulo, sinopse, capaUrl, status, generos, autor, idiomaOriginal);
    }

    private Capitulo parseCapitulo(final JsonNode node, final String mangaId) {
        final String id = node.path("id").asText();
        final JsonNode attr = node.path("attributes");
        final String numero = attr.path("chapter").asText("");
        final String titulo = attr.path("title").asText("");
        final String idioma = attr.path("translatedLanguage").asText("");
        final int paginas = attr.path("pages").asInt(0);
        return new Capitulo(id, mangaId, numero, titulo, idioma, paginas);
    }

    /**
     * Escolhe um texto localizado de um objeto {@code {"pt-br": "...", "en": "..."}},
     * preferindo pt-br, depois en, depois o primeiro disponível. Consulta também
     * uma lista de altTitles, quando fornecida.
     */
    private String escolherLocalizado(final JsonNode mapa, final JsonNode altTitles, final String padrao) {
        if (mapa != null && mapa.isObject()) {
            if (mapa.has(AppConfig.IDIOMA_PREFERIDO)) {
                return mapa.get(AppConfig.IDIOMA_PREFERIDO).asText();
            }
            if (mapa.has(AppConfig.IDIOMA_FALLBACK)) {
                return mapa.get(AppConfig.IDIOMA_FALLBACK).asText();
            }
        }
        if (altTitles != null && altTitles.isArray()) {
            for (final JsonNode alt : altTitles) {
                if (alt.has(AppConfig.IDIOMA_PREFERIDO)) {
                    return alt.get(AppConfig.IDIOMA_PREFERIDO).asText();
                }
            }
        }
        if (mapa != null && mapa.isObject()) {
            final var nomes = mapa.fieldNames();
            if (nomes.hasNext()) {
                return mapa.get(nomes.next()).asText();
            }
        }
        return padrao;
    }

    private static String encode(final String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}

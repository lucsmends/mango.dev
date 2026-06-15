package com.mango.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.config.Config;
import com.mango.db.CacheBuscaRepository;
import com.mango.exception.MangoException;
import com.mango.exception.RegraNegocioException;
import com.mango.model.Capitulo;
import com.mango.model.FiltroBusca;
import com.mango.model.Genero;
import com.mango.model.Manga;
import com.mango.model.ResultadoBusca;
import com.mango.net.JsonFetcher;
import com.mango.net.MangaDexHttp;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Serviço do catálogo (UC1): busca com filtros, populares, gêneros e
 * lista de capítulos da ficha.
 *
 * <p>Regras cobertas: RN1.1 (termo mínimo), RN1.2 (cache 30 min),
 * RN1.3 (paginação de 20), RN1.6 (pt-br com fallback en), FA1 (busca vazia
 * lista populares), FA2 (indicação de cache), EX4 (resultado vazio é
 * decidido na UI a partir de {@link ResultadoBusca#vazio()}).</p>
 */
public class CatalogoService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonFetcher http;
    private final CacheBuscaRepository cache;

    public CatalogoService() {
        this(new MangaDexHttp(), new CacheBuscaRepository());
    }

    /** Injeção para testes: {@code http} pode ser um simples lambda. */
    public CatalogoService(final JsonFetcher http, final CacheBuscaRepository cache) {
        this.http = http;
        this.cache = cache;
    }

    // ------------------------------------------------------------------
    // Busca (fluxo principal + FA1/FA2/FA3)
    // ------------------------------------------------------------------

    public ResultadoBusca buscar(final FiltroBusca filtro) {
        validar(filtro);

        final String chave = filtro.chaveCache();
        final var emCache = cache.buscar(chave);
        if (emCache.isPresent()) {                                    // FA2
            return parseResultado(emCache.get(), filtro).marcadoComoCache();
        }

        final JsonNode json = http.get(montarUrlBusca(filtro));
        cache.salvar(chave, json.toString());                         // RN1.2
        return parseResultado(json.toString(), filtro);
    }

    /** RN1.1 — termo opcional (FA1); se presente, mínimo de 2 caracteres não-brancos. */
    void validar(final FiltroBusca filtro) {
        if (!filtro.semTermo()
                && filtro.termo().replaceAll("\\s", "").length() < Config.TERMO_MINIMO) {
            throw new RegraNegocioException(
                    "Digite ao menos " + Config.TERMO_MINIMO + " caracteres para buscar.");
        }
    }

    String montarUrlBusca(final FiltroBusca filtro) {
        final StringBuilder url = new StringBuilder(Config.API_BASE)
                .append("/manga?limit=").append(Config.TAMANHO_PAGINA)        // RN1.3
                .append("&offset=").append(filtro.pagina() * Config.TAMANHO_PAGINA)
                .append("&includes[]=cover_art")
                .append("&contentRating[]=safe&contentRating[]=suggestive");

        if (filtro.semTermo()) {                                              // FA1
            url.append("&order[followedCount]=desc");
        } else {
            url.append("&title=")
               .append(URLEncoder.encode(filtro.termo(), StandardCharsets.UTF_8))
               .append("&order[relevance]=desc");
        }
        if (filtro.genero() != null) {                                        // MNG-32
            url.append("&includedTags[]=").append(filtro.genero().id());
        }
        return url.toString();
    }

    private ResultadoBusca parseResultado(final String payload, final FiltroBusca filtro) {
        final JsonNode root = lerJson(payload);
        final List<Manga> mangas = new ArrayList<>();
        for (final JsonNode item : root.path("data")) {
            mangas.add(parseManga(item));
        }
        return new ResultadoBusca(mangas, root.path("total").asInt(0),
                filtro.pagina(), false);
    }

    // ------------------------------------------------------------------
    // Gêneros (MNG-32)
    // ------------------------------------------------------------------

    /** Tags do grupo "genre" do MangaDex, ordenadas por nome. */
    public List<Genero> listarGeneros() {
        final JsonNode root = http.get(Config.API_BASE + "/manga/tag");
        final List<Genero> generos = new ArrayList<>();
        for (final JsonNode tag : root.path("data")) {
            final JsonNode attrs = tag.path("attributes");
            if ("genre".equals(attrs.path("group").asText())) {
                generos.add(new Genero(tag.path("id").asText(),
                        attrs.path("name").path("en").asText("?")));
            }
        }
        generos.sort((a, b) -> a.nome().compareToIgnoreCase(b.nome()));
        return generos;
    }

    // ------------------------------------------------------------------
    // Capítulos da ficha (RN1.6)
    // ------------------------------------------------------------------

    /** Capítulos em pt-br; sem nenhum, cai para en (RN1.6). */
    public List<Capitulo> listarCapitulos(final String mangaId) {
        List<Capitulo> caps = feed(mangaId, Config.IDIOMA_PREFERIDO);
        if (caps.isEmpty()) {
            caps = feed(mangaId, Config.IDIOMA_FALLBACK);
        }
        return caps;
    }

    private List<Capitulo> feed(final String mangaId, final String idioma) {
        final String url = Config.API_BASE + "/manga/" + mangaId
                + "/feed?limit=500&translatedLanguage[]=" + idioma
                + "&order[chapter]=asc&includeExternalUrl=0";
        final JsonNode root = http.get(url);
        final List<Capitulo> caps = new ArrayList<>();
        for (final JsonNode item : root.path("data")) {
            final JsonNode attrs = item.path("attributes");
            caps.add(new Capitulo(
                    item.path("id").asText(),
                    attrs.path("chapter").asText(""),
                    attrs.path("title").asText(""),
                    attrs.path("translatedLanguage").asText(idioma),
                    attrs.path("pages").asInt(0)));
        }
        return caps;
    }

    // ------------------------------------------------------------------
    // Parsing de mangá
    // ------------------------------------------------------------------

    private Manga parseManga(final JsonNode item) {
        final String id = item.path("id").asText();
        final JsonNode attrs = item.path("attributes");

        final String titulo = melhorTexto(attrs.path("title"),
                attrs.path("altTitles"));
        final String sinopse = textoPreferido(attrs.path("description"));
        final List<String> generos = new ArrayList<>();
        for (final JsonNode tag : attrs.path("tags")) {
            final JsonNode tagAttrs = tag.path("attributes");
            if ("genre".equals(tagAttrs.path("group").asText())) {
                generos.add(tagAttrs.path("name").path("en").asText());
            }
        }

        return new Manga(id, titulo, capaUrl(id, item),
                traduzirStatus(attrs.path("status").asText("")),
                attrs.hasNonNull("year") ? attrs.path("year").asInt() : null,
                sinopse,
                attrs.path("originalLanguage").asText(""),
                generos);
    }

    /** Título no idioma preferido; senão en, ja-ro, ou o primeiro disponível. */
    private String melhorTexto(final JsonNode title, final JsonNode altTitles) {
        final String direto = textoPreferido(title);
        if (!direto.isEmpty()) {
            return direto;
        }
        for (final JsonNode alt : altTitles) {
            final Iterator<Map.Entry<String, JsonNode>> campos = alt.fields();
            if (campos.hasNext()) {
                return campos.next().getValue().asText();
            }
        }
        return "(sem título)";
    }

    private String textoPreferido(final JsonNode mapa) {
        for (final String lang : new String[] {Config.IDIOMA_PREFERIDO, "en", "ja-ro"}) {
            if (mapa.hasNonNull(lang)) {
                return mapa.path(lang).asText();
            }
        }
        final Iterator<Map.Entry<String, JsonNode>> campos = mapa.fields();
        return campos.hasNext() ? campos.next().getValue().asText() : "";
    }

    private String capaUrl(final String mangaId, final JsonNode item) {
        for (final JsonNode rel : item.path("relationships")) {
            if ("cover_art".equals(rel.path("type").asText())) {
                final String arquivo = rel.path("attributes").path("fileName").asText("");
                if (!arquivo.isEmpty()) {
                    return Config.COVER_BASE + "/" + mangaId + "/" + arquivo + ".256.jpg";
                }
            }
        }
        return null;
    }

    private static String traduzirStatus(final String status) {
        return switch (status) {
            case "ongoing" -> "Em andamento";
            case "completed" -> "Concluído";
            case "hiatus" -> "Hiato";
            case "cancelled" -> "Cancelado";
            default -> status;
        };
    }

    private static JsonNode lerJson(final String payload) {
        try {
            return MAPPER.readTree(payload);
        } catch (final IOException e) {
            throw new MangoException("Payload de busca inválido.", e);
        }
    }
}

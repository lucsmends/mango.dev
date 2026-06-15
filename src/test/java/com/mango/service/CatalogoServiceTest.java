package com.mango.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.db.CacheBuscaRepository;
import com.mango.exception.RegraNegocioException;
import com.mango.model.FiltroBusca;
import com.mango.model.Genero;
import com.mango.model.ResultadoBusca;
import com.mango.net.JsonFetcher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regras de negócio do UC1 sem rede e sem banco: o {@link JsonFetcher} é um
 * lambda e o cache é um stub em memória.
 */
class CatalogoServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String BUSCA_JSON = """
            { "total": 45, "data": [ {
                "id": "m1",
                "attributes": {
                    "title": { "en": "One Piece" },
                    "altTitles": [],
                    "description": { "pt-br": "Piratas." },
                    "status": "ongoing",
                    "year": 1997,
                    "originalLanguage": "ja",
                    "tags": [ { "attributes":
                        { "group": "genre", "name": { "en": "Action" } } } ]
                },
                "relationships": [ { "type": "cover_art",
                    "attributes": { "fileName": "capa.jpg" } } ]
            } ] }""";

    /** Cache em memória, sem H2. */
    private static class CacheStub extends CacheBuscaRepository {
        final Map<String, String> mapa = new HashMap<>();

        @Override
        public Optional<String> buscar(final String chave) {
            return Optional.ofNullable(mapa.get(chave));
        }

        @Override
        public void salvar(final String chave, final String payload) {
            mapa.put(chave, payload);
        }
    }

    private static JsonNode json(final String s) {
        try {
            return MAPPER.readTree(s);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- RN1.1 ----------

    @Test
    void rn11_termoComMenosDe2CaracteresNaoBrancosEhBloqueado() {
        final CatalogoService service = new CatalogoService(u -> json("{}"), new CacheStub());
        assertThrows(RegraNegocioException.class,
                () -> service.buscar(new FiltroBusca("a", null, 0)));
        assertThrows(RegraNegocioException.class,
                () -> service.buscar(new FiltroBusca("  a  ", null, 0)));
    }

    @Test
    void fa1_buscaSemTermoListaPopulares() {
        final List<String> urls = new ArrayList<>();
        final CatalogoService service = new CatalogoService(u -> {
            urls.add(u);
            return json(BUSCA_JSON);
        }, new CacheStub());

        final ResultadoBusca r = service.buscar(new FiltroBusca("", null, 0));

        assertFalse(r.vazio());
        assertTrue(urls.get(0).contains("order[followedCount]=desc"));
    }

    // ---------- RN1.3 ----------

    @Test
    void rn13_urlUsaLimite20EOffsetDaPagina() {
        final CatalogoService service = new CatalogoService(u -> json("{}"), new CacheStub());
        final String url = service.montarUrlBusca(new FiltroBusca("naruto", null, 2));
        assertTrue(url.contains("limit=20"));
        assertTrue(url.contains("offset=40"));
    }

    @Test
    void mng32_filtroDeGeneroEntraNaUrl() {
        final CatalogoService service = new CatalogoService(u -> json("{}"), new CacheStub());
        final String url = service.montarUrlBusca(
                new FiltroBusca("naruto", new Genero("uuid-acao", "Action"), 0));
        assertTrue(url.contains("includedTags[]=uuid-acao"));
    }

    // ---------- RN1.2 / FA2 ----------

    @Test
    void rn12_segundaBuscaIgualVemDoCacheSemChamarApi() {
        final List<String> urls = new ArrayList<>();
        final CacheStub cache = new CacheStub();
        final CatalogoService service = new CatalogoService(u -> {
            urls.add(u);
            return json(BUSCA_JSON);
        }, cache);
        final FiltroBusca filtro = new FiltroBusca("one piece", null, 0);

        final ResultadoBusca primeira = service.buscar(filtro);
        final ResultadoBusca segunda = service.buscar(filtro);

        assertEquals(1, urls.size());          // API chamada uma única vez
        assertFalse(primeira.doCache());
        assertTrue(segunda.doCache());         // FA2: indicado como cache
    }

    // ---------- parsing ----------

    @Test
    void parsingDeMangaPreencheTituloCapaStatusEGeneros() {
        final CatalogoService service =
                new CatalogoService(u -> json(BUSCA_JSON), new CacheStub());

        final ResultadoBusca r = service.buscar(new FiltroBusca("one piece", null, 0));
        final var manga = r.mangas().get(0);

        assertEquals("One Piece", manga.titulo());
        assertEquals("Em andamento", manga.status());
        assertEquals(List.of("Action"), manga.generos());
        assertTrue(manga.capaUrl().endsWith("/m1/capa.jpg.256.jpg"));
        assertEquals(45, r.total());
        assertEquals(3, r.totalPaginas());     // RN1.3: 45 itens → 3 páginas
    }

    // ---------- RN1.6 ----------

    @Test
    void rn16_semCapitulosPtBrCaiParaIngles() {
        final String feedVazio = "{ \"data\": [] }";
        final String feedEn = """
                { "data": [ { "id": "c1", "attributes": {
                    "chapter": "1", "title": "Romance Dawn",
                    "translatedLanguage": "en", "pages": 50 } } ] }""";
        final CatalogoService service = new CatalogoService(u ->
                json(u.contains("translatedLanguage[]=pt-br") ? feedVazio : feedEn),
                new CacheStub());

        final var caps = service.listarCapitulos("m1");

        assertEquals(1, caps.size());
        assertEquals("en", caps.get(0).idioma());
    }
}

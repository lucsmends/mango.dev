package com.mango.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.exception.MangaDexException;
import com.mango.exception.RegraNegocioException;
import com.mango.model.Capitulo;
import com.mango.model.FiltroBusca;
import com.mango.model.ResultadoBusca;
import com.mango.repository.CacheBuscaRepository;
import com.mango.util.HttpJsonClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link MangaDexService}, cobrindo as RNs do UC1 com
 * o cliente HTTP e o cache mockados (sem rede nem banco).
 *
 * <p>Rastreabilidade: RN1.1 (termo mínimo), RN1.2/FA2 (cache), RN1.3 (paginação),
 * RN1.6 (dedupe por idioma), EX2 (fallback de cache), EX4 (resultado vazio).</p>
 */
@ExtendWith(MockitoExtension.class)
class MangaDexServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock private HttpJsonClient http;
    @Mock private CacheBuscaRepository cache;

    private MangaDexService service() {
        return new MangaDexService(http, cache);
    }

    @Test
    void termoComUmCaractereVioleRN11() {
        final var service = service();
        final var ex = assertThrows(RegraNegocioException.class,
                () -> service.buscar(FiltroBusca.porTitulo("a")));
        assertTrue(ex.getMessage().contains("2 caracteres"));
        verify(http, never()).getJson(anyString());
    }

    @Test
    void termoVazioListaPopularesESalvaCache() throws Exception {
        when(cache.buscar(anyString())).thenReturn(Optional.empty());
        when(http.getJson(anyString())).thenReturn(umMangaJson());

        final ResultadoBusca r = service().buscar(FiltroBusca.porTitulo(""));

        assertEquals(1, r.mangas().size());
        assertEquals("Naruto", r.mangas().get(0).titulo());
        assertFalse(r.doCache());
        verify(cache).salvar(anyString(), anyString());
    }

    @Test
    void resultadoDoCacheNaoChamaApi() throws Exception {
        when(cache.buscar(anyString())).thenReturn(Optional.of(umMangaJson().toString()));

        final ResultadoBusca r = service().buscar(FiltroBusca.porTitulo("naruto"));

        assertTrue(r.doCache());                       // FA2
        verify(http, never()).getJson(anyString());
    }

    @Test
    void paginacaoCalculaTotalDePaginas() {
        final ResultadoBusca r = new ResultadoBusca(List.of(), 0, 45, 20, false);
        assertEquals(3, r.totalPaginas());             // RN1.3
        assertTrue(r.temProxima());
        assertFalse(r.temAnterior());
    }

    @Test
    void apiForaServeCacheExpirado() throws Exception {
        // EX2: API falha, mas há um resultado antigo em cache -> degradação graciosa.
        when(cache.buscar(anyString())).thenReturn(Optional.empty());
        when(http.getJson(anyString())).thenThrow(new MangaDexException("indisponível"));
        when(cache.buscarIgnorandoValidade(anyString()))
                .thenReturn(Optional.of(umMangaJson().toString()));

        final ResultadoBusca r = service().buscar(FiltroBusca.porTitulo("naruto"));

        assertTrue(r.doCache());
        assertEquals(1, r.mangas().size());
    }

    @Test
    void semCacheApiForaPropagaErro() {
        when(cache.buscar(anyString())).thenReturn(Optional.empty());
        when(http.getJson(anyString())).thenThrow(new MangaDexException("indisponível"));
        when(cache.buscarIgnorandoValidade(anyString())).thenReturn(Optional.empty());

        assertThrows(MangaDexException.class,
                () -> service().buscar(FiltroBusca.porTitulo("naruto")));
    }

    @Test
    void listarCapitulosDeduplicaPreferindoPtBr() throws Exception {
        when(http.getJson(anyString())).thenReturn(feedComDuplicatas());

        final List<Capitulo> capitulos = service().listarCapitulos("abc-123");

        // Capítulo "1" existe em en e pt-br -> deve sobrar 1 (pt-br); "2" só em en.
        assertEquals(2, capitulos.size());
        final Capitulo cap1 = capitulos.stream()
                .filter(c -> "1".equals(c.numero())).findFirst().orElseThrow();
        assertEquals("pt-br", cap1.idioma());          // RN1.6
    }

    // ------------------------------------------------------------- fixtures

    private JsonNode umMangaJson() throws Exception {
        final String json = """
                {
                  "result": "ok",
                  "total": 1,
                  "data": [
                    {
                      "id": "abc-123",
                      "attributes": {
                        "title": { "en": "Naruto" },
                        "description": { "pt-br": "Ninja." },
                        "status": "completed",
                        "originalLanguage": "ja",
                        "tags": [
                          { "attributes": { "group": "genre", "name": { "en": "Action" } } }
                        ]
                      },
                      "relationships": [
                        { "type": "author", "attributes": { "name": "Kishimoto" } },
                        { "type": "cover_art", "attributes": { "fileName": "cover.jpg" } }
                      ]
                    }
                  ]
                }
                """;
        return MAPPER.readTree(json);
    }

    private JsonNode feedComDuplicatas() throws Exception {
        final String json = """
                {
                  "result": "ok",
                  "total": 3,
                  "data": [
                    { "id": "c1-en",  "attributes": { "chapter": "1", "title": "A", "translatedLanguage": "en",    "pages": 10 } },
                    { "id": "c1-ptbr","attributes": { "chapter": "1", "title": "A", "translatedLanguage": "pt-br", "pages": 10 } },
                    { "id": "c2-en",  "attributes": { "chapter": "2", "title": "B", "translatedLanguage": "en",    "pages": 12 } }
                  ]
                }
                """;
        return MAPPER.readTree(json);
    }
}

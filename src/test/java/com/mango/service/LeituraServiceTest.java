package com.mango.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.exception.MangaDexException;
import com.mango.model.Pagina;
import com.mango.repository.HistoricoRepository;
import com.mango.util.HttpJsonClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeituraServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock private HttpJsonClient http;
    @Mock private HistoricoRepository historico;

    private LeituraService service() {
        return new LeituraService(http, historico);
    }

    @Test
    void carregarPaginasMontaUrlsCorretas() throws Exception {
        when(http.getJson(anyString())).thenReturn(atHomeJson());

        final List<Pagina> paginas = service().carregarPaginas("cap-1");

        assertEquals(2, paginas.size());
        assertEquals("https://uploads.mangadex.org/data/HASH/p1.png", paginas.get(0).url());
        assertEquals(1, paginas.get(0).numero());
        assertEquals(2, paginas.get(1).numero());
    }

    @Test
    void capituloSemPaginasLancaExcecao() throws Exception {
        when(http.getJson(anyString())).thenReturn(
                MAPPER.readTree("{\"baseUrl\":\"x\",\"chapter\":{\"hash\":\"h\",\"data\":[]}}"));

        assertThrows(MangaDexException.class, () -> service().carregarPaginas("cap-vazio"));
    }

    @Test
    void registrarProgressoMarcaConcluidoNaUltimaPagina() {
        service().registrarProgresso("m1", "c1", 4, 5);     // ultima (indice 4 de 5)
        verify(historico).salvar("m1", "c1", 4, 5, true);   // RN2.3
    }

    @Test
    void registrarProgressoNaoMarcaConcluidoNoMeio() {
        service().registrarProgresso("m1", "c1", 1, 5);
        verify(historico).salvar("m1", "c1", 1, 5, false);  // RN2.2
    }

    private JsonNode atHomeJson() throws Exception {
        final String json = """
                {
                  "result": "ok",
                  "baseUrl": "https://uploads.mangadex.org",
                  "chapter": {
                    "hash": "HASH",
                    "data": [ "p1.png", "p2.png" ],
                    "dataSaver": []
                  }
                }
                """;
        return MAPPER.readTree(json);
    }
}

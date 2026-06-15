package com.mango.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.db.ProgressoRepository;
import com.mango.exception.ApiException;
import com.mango.model.Capitulo;
import com.mango.model.Manga;
import com.mango.model.Progresso;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras do UC2 sem rede e sem banco. */
class LeitorServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(final String s) {
        try {
            return MAPPER.readTree(s);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Manga MANGA =
            new Manga("m1", "One Piece", "capa.jpg", "Em andamento", 1997, "Piratas.", "ja", List.of());
    private static final Capitulo CAP =
            new Capitulo("c1", "12", "Romance Dawn", "pt-br", 10);

    /** Progresso em memória, sem H2. */
    private static class ProgressoStub extends ProgressoRepository {
        final Map<String, Progresso> mapa = new HashMap<>();

        @Override
        public void salvar(final String mangaId, final String capituloId,
                           final String titulo, final String capaUrl, final String capNumero,
                           final int paginaAtual, final int totalPaginas, final boolean concluido) {
            mapa.put(mangaId + "/" + capituloId,
                    new Progresso(paginaAtual, totalPaginas, concluido));
        }

        @Override
        public Optional<Progresso> buscar(final String mangaId, final String capituloId) {
            return Optional.ofNullable(mapa.get(mangaId + "/" + capituloId));
        }
    }

    @Test
    void montaUrlsDasPaginasAPartirDoEndpointAtHome() {
        final String atHome = """
                { "baseUrl": "https://srv.mangadex.org",
                  "chapter": { "hash": "abc", "data": ["p1.png", "p2.png"] } }""";
        final LeitorService service =
                new LeitorService(u -> json(atHome), new ProgressoStub());

        final var paginas = service.paginas("c1");

        assertEquals(2, paginas.size());
        assertEquals(1, paginas.get(0).numero());
        assertEquals("https://srv.mangadex.org/data/abc/p1.png", paginas.get(0).url());
    }

    @Test
    void ex2_capituloSemPaginasLancaExcecaoComMensagemDaSpec() {
        final String vazio = """
                { "baseUrl": "x", "chapter": { "hash": "h", "data": [] } }""";
        final LeitorService service =
                new LeitorService(u -> json(vazio), new ProgressoStub());

        final ApiException ex =
                assertThrows(ApiException.class, () -> service.paginas("c1"));
        assertTrue(ex.getMessage().contains("ainda não está disponível"));
    }

    @Test
    void rn22_progressoEhPersistidoACadaPagina() {
        final ProgressoStub repo = new ProgressoStub();
        final LeitorService service = new LeitorService(u -> json("{}"), repo);

        service.salvarProgresso(MANGA, CAP, 3, 10);

        final Progresso p = repo.mapa.get("m1/c1");
        assertEquals(3, p.paginaAtual());
        assertFalse(p.concluido());
    }

    @Test
    void rn23_ultimaPaginaMarcaCapituloComoConcluido() {
        final ProgressoStub repo = new ProgressoStub();
        final LeitorService service = new LeitorService(u -> json("{}"), repo);

        service.salvarProgresso(MANGA, CAP, 9, 10);   // zero-based: página 10 de 10

        assertTrue(repo.mapa.get("m1/c1").concluido());
    }

    @Test
    void fa5_progressoSalvoEhRecuperadoParaRetomada() {
        final ProgressoStub repo = new ProgressoStub();
        final LeitorService service = new LeitorService(u -> json("{}"), repo);

        service.salvarProgresso(MANGA, CAP, 4, 10);

        final Optional<Progresso> p = service.progressoSalvo("m1", "c1");
        assertTrue(p.isPresent());
        assertEquals(4, p.get().paginaAtual());
    }
}

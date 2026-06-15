package com.mango.service;

import com.mango.db.RelatorioRepository;
import com.mango.exception.RegraNegocioException;
import com.mango.model.RegistroLeitura;
import com.mango.model.Relatorio;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras dos relatórios do UC3 sem banco: o repositório é um stub em memória. */
class RelatorioServiceTest {

    /** Stub com dados fixos de leitura e gêneros. */
    private static class RepoStub extends RelatorioRepository {
        @Override
        public List<RegistroLeitura> leituras(final Instant de, final Instant ate) {
            return List.of(
                    new RegistroLeitura("m1", "One Piece", 4, 10, false),  // 5 págs, em andamento
                    new RegistroLeitura("m1", "One Piece", 9, 10, true),   // 10 págs, concluído
                    new RegistroLeitura("m2", "Naruto", 0, 20, true));     // 20 págs, concluído
        }

        @Override
        public List<String> generosBiblioteca() {
            return List.of("Action", "Action", "Comedy");
        }
    }

    private static Relatorio gerar() {
        return new RelatorioService(new RepoStub()).gerar(null, null);
    }

    @Test
    void agregaCapitulosPaginasEMangasDistintos() {
        final Relatorio r = gerar();
        assertEquals(2, r.mangasLidos());            // m1 e m2
        assertEquals(2, r.capitulosConcluidos());
        assertEquals(1, r.capitulosEmAndamento());
        assertEquals(35, r.paginasLidas());          // 5 + 10 + 20
    }

    @Test
    void rn34_tempoEstimadoEh30sPorPagina() {
        // 35 páginas × 30s = 1050s = 17 min
        assertEquals(17, gerar().minutosLeitura());
    }

    @Test
    void topMangasOrdenadoPorPaginasLidas() {
        final var top = gerar().topMangas();
        assertEquals("Naruto", top.get(0).rotulo());     // 20 páginas
        assertEquals(20, top.get(0).valor());
        assertEquals("One Piece", top.get(1).rotulo());  // 5 + 10 = 15
        assertEquals(15, top.get(1).valor());
    }

    @Test
    void generosFavoritosContamOcorrenciasDaBiblioteca() {
        final var generos = gerar().topGeneros();
        assertEquals("Action", generos.get(0).rotulo());
        assertEquals(2, generos.get(0).valor());
        assertEquals("Comedy", generos.get(1).rotulo());
    }

    @Test
    void ex2_dataInicialPosteriorAFinalEhBloqueada() {
        final RelatorioService service = new RelatorioService(new RepoStub());
        final Instant agora = Instant.now();
        assertThrows(RegraNegocioException.class,
                () -> service.gerar(agora, agora.minusSeconds(3600)));
    }

    @Test
    void rn36_csvComeçaComBomEContemOsDados() {
        final String csv = new RelatorioService(new RepoStub()).exportarCsv(gerar());
        assertTrue(csv.startsWith("﻿"), "CSV deve começar com BOM UTF-8");
        assertTrue(csv.contains("Mangás lidos;2"));
        assertTrue(csv.contains("Naruto;20"));
        assertTrue(csv.contains("Action;2"));
    }
}

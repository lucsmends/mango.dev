package com.mango.service;

import com.mango.db.ProgressoRepository;
import com.mango.model.LeituraRecente;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Histórico e "continuar lendo" (UC3) sem banco. */
class HistoricoServiceTest {

    private static LeituraRecente leitura(final String id, final boolean concluido) {
        return new LeituraRecente(id, "c1", "Título", "capa.jpg", "1", 0, 10, concluido);
    }

    private static class ProgressoStub extends ProgressoRepository {
        final List<LeituraRecente> historico = List.of(leitura("m1", true), leitura("m2", false));
        final List<LeituraRecente> emAndamento = List.of(leitura("m2", false));

        @Override
        public List<LeituraRecente> listarHistorico() {
            return historico;
        }

        @Override
        public List<LeituraRecente> listarEmAndamento() {
            return emAndamento;
        }
    }

    @Test
    void historicoDelegaAoRepositorio() {
        final ProgressoStub repo = new ProgressoStub();
        assertSame(repo.historico, new HistoricoService(repo).historico());
    }

    @Test
    void rn35_continuarLendoTrazApenasOsEmAndamento() {
        final ProgressoStub repo = new ProgressoStub();
        final List<LeituraRecente> r = new HistoricoService(repo).continuarLendo();
        assertEquals(1, r.size());
        assertEquals("m2", r.get(0).mangaId());
    }
}

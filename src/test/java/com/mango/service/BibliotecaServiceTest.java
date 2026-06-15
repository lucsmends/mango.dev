package com.mango.service;

import com.mango.db.BibliotecaRepository;
import com.mango.db.ColecaoRepository;
import com.mango.exception.RegraNegocioException;
import com.mango.model.Colecao;
import com.mango.model.ItemBiblioteca;
import com.mango.model.Manga;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras da biblioteca (UC3) sem banco: repositórios são stubs em memória. */
class BibliotecaServiceTest {

    private static class ColecaoStub extends ColecaoRepository {
        boolean nomeExiste;
        Long removida;
        String criada;

        @Override
        public boolean existeNome(final String nome) {
            return nomeExiste;
        }

        @Override
        public long criar(final String nome) {
            criada = nome;
            return 42L;
        }

        @Override
        public void remover(final long id) {
            removida = id;
        }
    }

    private static class BibliotecaStub extends BibliotecaRepository {
        List<String> generosRecebidos;

        @Override
        public void adicionar(final long colecaoId, final ItemBiblioteca item, final List<String> generos) {
            this.generosRecebidos = generos;
        }
    }

    // ---------- RN3.2: nome de coleção ----------

    @Test
    void rn32_nomeMuitoCurtoEhBloqueado() {
        final BibliotecaService service = new BibliotecaService(new ColecaoStub(), new BibliotecaStub());
        assertThrows(RegraNegocioException.class, () -> service.criarColecao("a"));
    }

    @Test
    void rn32_nomeMuitoLongoEhBloqueado() {
        final BibliotecaService service = new BibliotecaService(new ColecaoStub(), new BibliotecaStub());
        assertThrows(RegraNegocioException.class, () -> service.criarColecao("x".repeat(41)));
    }

    @Test
    void rn32_nomeDuplicadoEhBloqueado() {
        final ColecaoStub colecoes = new ColecaoStub();
        colecoes.nomeExiste = true;
        final BibliotecaService service = new BibliotecaService(colecoes, new BibliotecaStub());
        assertThrows(RegraNegocioException.class, () -> service.criarColecao("Shounen"));
    }

    @Test
    void criarColecaoValidaPersisteERetornaRemovivel() {
        final ColecaoStub colecoes = new ColecaoStub();
        final BibliotecaService service = new BibliotecaService(colecoes, new BibliotecaStub());

        final Colecao nova = service.criarColecao("  Shounen  ");   // strip aplicado

        assertEquals("Shounen", colecoes.criada);
        assertEquals("Shounen", nova.nome());
        assertTrue(nova.removivel());
    }

    // ---------- RN3.3: coleções padrão ----------

    @Test
    void rn33_colecaoNaoRemovivelNaoPodeSerExcluida() {
        final ColecaoStub colecoes = new ColecaoStub();
        final BibliotecaService service = new BibliotecaService(colecoes, new BibliotecaStub());
        final Colecao favoritos = new Colecao(1L, "Favoritos", false);

        assertThrows(RegraNegocioException.class, () -> service.removerColecao(favoritos));
        assertEquals(null, colecoes.removida);
    }

    @Test
    void colecaoRemovivelEhExcluida() {
        final ColecaoStub colecoes = new ColecaoStub();
        final BibliotecaService service = new BibliotecaService(colecoes, new BibliotecaStub());
        service.removerColecao(new Colecao(7L, "Shounen", true));
        assertEquals(7L, colecoes.removida);
    }

    // ---------- gêneros persistidos para o relatório ----------

    @Test
    void adicionarRepassaOsGenerosDoMangaParaORepositorio() {
        final BibliotecaStub itens = new BibliotecaStub();
        final BibliotecaService service = new BibliotecaService(new ColecaoStub(), itens);
        final Manga manga = new Manga("m1", "One Piece", "capa.jpg", "Em andamento",
                1997, "Piratas.", "ja", List.of("Action", "Adventure"));

        service.adicionar(1L, manga);

        assertEquals(List.of("Action", "Adventure"), itens.generosRecebidos);
    }

    @Test
    void rn33_listarColecoesDelegaAoRepositorio() {
        final ColecaoStub colecoes = new ColecaoStub() {
            @Override
            public List<Colecao> listar() {
                return List.of(new Colecao(1L, "Favoritos", false));
            }
        };
        final BibliotecaService service = new BibliotecaService(colecoes, new BibliotecaStub());
        assertFalse(service.listarColecoes().isEmpty());
    }
}

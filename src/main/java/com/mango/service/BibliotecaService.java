package com.mango.service;

import com.mango.db.BibliotecaRepository;
import com.mango.db.ColecaoRepository;
import com.mango.exception.RegraNegocioException;
import com.mango.model.Colecao;
import com.mango.model.ItemBiblioteca;
import com.mango.model.Manga;

import java.util.List;
import java.util.Set;

/** Regras da biblioteca pessoal (UC3): coleções e itens. */
public class BibliotecaService {

    private final ColecaoRepository colecoes;
    private final BibliotecaRepository itens;

    public BibliotecaService() {
        this(new ColecaoRepository(), new BibliotecaRepository());
    }

    /** Injeção para testes: repositórios podem ser stubs em memória. */
    public BibliotecaService(final ColecaoRepository colecoes, final BibliotecaRepository itens) {
        this.colecoes = colecoes;
        this.itens = itens;
    }

    public List<Colecao> listarColecoes() {
        return colecoes.listar();
    }

    /** RN3.2: nome de 2 a 40 caracteres, único (case-insensitive). */
    public Colecao criarColecao(final String nomeBruto) {
        final String nome = nomeBruto == null ? "" : nomeBruto.strip();
        if (nome.length() < 2 || nome.length() > 40) {
            throw new RegraNegocioException("O nome da lista deve ter de 2 a 40 caracteres.");
        }
        if (colecoes.existeNome(nome)) {
            throw new RegraNegocioException("Já existe uma lista com esse nome.");
        }
        return new Colecao(colecoes.criar(nome), nome, true);
    }

    /** RN3.3: coleções padrão não podem ser removidas. */
    public void removerColecao(final Colecao colecao) {
        if (!colecao.removivel()) {
            throw new RegraNegocioException("As listas padrão não podem ser removidas.");
        }
        colecoes.remover(colecao.id());
    }

    public void adicionar(final long colecaoId, final Manga manga) {
        itens.adicionar(colecaoId,
                new ItemBiblioteca(manga.id(), manga.titulo(), manga.capaUrl()),
                manga.generos());
    }

    public void remover(final long colecaoId, final String mangaId) {
        itens.remover(colecaoId, mangaId);
    }

    public List<ItemBiblioteca> itens(final long colecaoId) {
        return itens.listarItens(colecaoId);
    }

    public Set<Long> colecoesDoManga(final String mangaId) {
        return itens.colecoesDoManga(mangaId);
    }
}

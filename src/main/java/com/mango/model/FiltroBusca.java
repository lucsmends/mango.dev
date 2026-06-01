package com.mango.model;

import java.util.List;

/**
 * Parâmetros de uma busca no catálogo (UC1).
 *
 * @param termo   termo de pesquisa por título (pode ser vazio = catálogo geral, FA1)
 * @param generos gêneros selecionados (atualmente reservado para evolução do MNG-32)
 * @param autor   autor (reservado para evolução do MNG-32)
 * @param pagina  página solicitada, base 0 (RN1.3)
 */
public record FiltroBusca(
        String termo,
        List<String> generos,
        String autor,
        int pagina) {

    public FiltroBusca {
        termo = termo == null ? "" : termo.trim();
        generos = generos == null ? List.of() : List.copyOf(generos);
        autor = autor == null ? "" : autor.trim();
        if (pagina < 0) {
            pagina = 0;
        }
    }

    /** Construtor de conveniência para busca simples por título na primeira página. */
    public static FiltroBusca porTitulo(final String termo) {
        return new FiltroBusca(termo, List.of(), "", 0);
    }

    /** Cria uma cópia apontando para outra página, preservando os filtros. */
    public FiltroBusca naPagina(final int novaPagina) {
        return new FiltroBusca(termo, generos, autor, novaPagina);
    }

    /** Chave estável usada para indexar o cache de buscas (RN1.2). */
    public String chaveCache() {
        return String.join("|",
                termo.toLowerCase(),
                String.join(",", generos).toLowerCase(),
                autor.toLowerCase(),
                Integer.toString(pagina));
    }
}

package com.mango.model;

import java.util.List;

/**
 * Resultado paginado de uma busca no catálogo (UC1).
 *
 * @param mangas       mangás da página atual
 * @param pagina       índice da página atual (base 0)
 * @param total        total de resultados informado pela API
 * @param tamanhoPagina itens por página (RN1.3)
 * @param doCache      indica se o resultado veio do cache local (FA2)
 */
public record ResultadoBusca(
        List<Manga> mangas,
        int pagina,
        int total,
        int tamanhoPagina,
        boolean doCache) {

    public ResultadoBusca {
        mangas = mangas == null ? List.of() : List.copyOf(mangas);
    }

    /** Número total de páginas, no mínimo 1. */
    public int totalPaginas() {
        if (total <= 0 || tamanhoPagina <= 0) {
            return 1;
        }
        return (int) Math.ceil((double) total / tamanhoPagina);
    }

    public boolean temProxima() {
        return pagina + 1 < totalPaginas();
    }

    public boolean temAnterior() {
        return pagina > 0;
    }

    public boolean vazio() {
        return mangas.isEmpty();
    }
}

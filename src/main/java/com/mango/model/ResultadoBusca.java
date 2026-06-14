package com.mango.model;

import com.mango.config.Config;

import java.util.List;

/** Página de resultados de busca (UC1), com metadados de paginação (RN1.3). */
public record ResultadoBusca(
        List<Manga> mangas,
        int total,
        int pagina,
        boolean doCache) {

    public ResultadoBusca {
        mangas = mangas == null ? List.of() : List.copyOf(mangas);
    }

    public int totalPaginas() {
        return (int) Math.ceil((double) total / Config.TAMANHO_PAGINA);
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

    /** Mesmo resultado, marcado como vindo do cache (FA2). */
    public ResultadoBusca marcadoComoCache() {
        return new ResultadoBusca(mangas, total, pagina, true);
    }
}

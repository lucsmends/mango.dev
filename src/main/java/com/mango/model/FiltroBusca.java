package com.mango.model;

/**
 * Combinação de filtros de uma busca (UC1): termo, gênero e página.
 * A chave de cache (RN1.2) é derivada exatamente desta combinação.
 */
public record FiltroBusca(String termo, Genero genero, int pagina) {

    public FiltroBusca {
        termo = termo == null ? "" : termo.strip();
        if (pagina < 0) {
            pagina = 0;
        }
    }

    public boolean semTermo() {
        return termo.isEmpty();
    }

    /** Chave única por combinação de filtros (RN1.2). */
    public String chaveCache() {
        final String gen = genero == null ? "-" : genero.id();
        return termo.toLowerCase() + "|" + gen + "|" + pagina;
    }

    public FiltroBusca comPagina(final int novaPagina) {
        return new FiltroBusca(termo, genero, novaPagina);
    }
}

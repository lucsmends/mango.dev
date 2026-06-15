package com.mango.model;

/** Linha bruta de progresso usada para agregar relatórios (UC3). */
public record RegistroLeitura(
        String mangaId,
        String titulo,
        int paginaAtual,
        int totalPaginas,
        boolean concluido) {

    /** RN3.4 — páginas efetivamente exibidas (base do tempo de leitura). */
    public int paginasLidas() {
        final int lidas = concluido ? totalPaginas : paginaAtual + 1;
        return Math.max(lidas, 0);
    }
}

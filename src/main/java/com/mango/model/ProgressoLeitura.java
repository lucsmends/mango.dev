package com.mango.model;

/**
 * Progresso de leitura de um capitulo (UC2 / UC3).
 *
 * @param paginaAtual  indice da ultima pagina vista (0-based)
 * @param totalPaginas total de paginas do capitulo
 * @param concluido    se o capitulo foi lido ate o fim (RN2.3)
 */
public record ProgressoLeitura(int paginaAtual, int totalPaginas, boolean concluido) {
}

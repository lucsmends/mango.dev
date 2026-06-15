package com.mango.model;

/** Progresso de leitura persistido (RN2.2/RN2.3, FA5 do UC2). */
public record Progresso(int paginaAtual, int totalPaginas, boolean concluido) {
}

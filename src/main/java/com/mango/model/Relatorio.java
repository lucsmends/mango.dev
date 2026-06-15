package com.mango.model;

import java.util.List;

/** Estatísticas de leitura agregadas (UC3 — relatórios). */
public record Relatorio(
        int mangasLidos,
        int capitulosConcluidos,
        int capitulosEmAndamento,
        int paginasLidas,
        long minutosLeitura,
        List<Contagem> topMangas,
        List<Contagem> topGeneros) {

    /** Par rótulo/valor para rankings (mangás mais lidos, gêneros favoritos). */
    public record Contagem(String rotulo, long valor) {
    }

    /** Tempo de leitura formatado como "Xh Ymin". */
    public String tempoFormatado() {
        final long h = minutosLeitura / 60;
        final long min = minutosLeitura % 60;
        return h > 0 ? h + "h " + min + "min" : min + "min";
    }
}

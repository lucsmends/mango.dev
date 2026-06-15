package com.mango.model;

/** Capítulo listado na ficha do mangá (UC1) e aberto pelo leitor (UC2). */
public record Capitulo(
        String id,
        String numero,
        String titulo,
        String idioma,
        int totalPaginas) {

    /** Rótulo exibido na lista de capítulos. */
    public String rotulo() {
        final String num = (numero == null || numero.isBlank()) ? "—" : numero;
        final String tit = (titulo == null || titulo.isBlank()) ? "" : " — " + titulo;
        return "Capítulo " + num + tit + "  [" + idioma + "]";
    }
}

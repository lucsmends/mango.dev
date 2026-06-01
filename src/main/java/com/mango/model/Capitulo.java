package com.mango.model;

/**
 * Representa um capítulo de um mangá (entidade de domínio imutável).
 *
 * @param id      identificador (UUID) do capítulo no MangaDex
 * @param mangaId identificador do mangá ao qual pertence
 * @param numero  número do capítulo (string, pois pode ser "10.5")
 * @param titulo  título do capítulo (pode ser vazio)
 * @param idioma  idioma traduzido (ex.: "pt-br", "en")
 * @param paginas quantidade de páginas declarada pela API
 */
public record Capitulo(
        String id,
        String mangaId,
        String numero,
        String titulo,
        String idioma,
        int paginas) {

    /** Rótulo amigável para exibição em lista (ex.: "Cap. 12 — Título"). */
    public String rotulo() {
        final String base = "Cap. " + (numero == null || numero.isBlank() ? "?" : numero);
        return (titulo == null || titulo.isBlank()) ? base : base + " — " + titulo;
    }
}

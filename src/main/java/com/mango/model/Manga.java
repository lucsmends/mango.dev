package com.mango.model;

import java.util.List;

/**
 * Representa um mangá do catálogo do MangaDex (entidade de domínio imutável).
 *
 * @param id            identificador (UUID) do mangá no MangaDex
 * @param titulo        título no idioma preferido, com fallback
 * @param sinopse       descrição/sinopse
 * @param capaUrl       URL da imagem de capa (pode ser {@code null})
 * @param status        status de publicação (ongoing, completed, hiatus...)
 * @param generos       lista de gêneros/tags
 * @param autor         nome do autor principal (pode ser {@code null})
 * @param idiomaOriginal idioma original (ex.: "ja", "ko")
 */
public record Manga(
        String id,
        String titulo,
        String sinopse,
        String capaUrl,
        String status,
        List<String> generos,
        String autor,
        String idiomaOriginal) {

    public Manga {
        generos = generos == null ? List.of() : List.copyOf(generos);
    }
}

package com.mango.model;

import java.util.List;

/** Mangá do catálogo (UC1). Imutável. */
public record Manga(
        String id,
        String titulo,
        String capaUrl,
        String status,
        Integer ano,
        String sinopse,
        String idiomaOriginal,
        List<String> generos) {

    public Manga {
        generos = generos == null ? List.of() : List.copyOf(generos);
    }

    /** RN2.5 — mangás coreanos abrem em scroll vertical por padrão. */
    public boolean coreano() {
        return "ko".equalsIgnoreCase(idiomaOriginal);
    }
}

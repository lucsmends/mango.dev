package com.mango.model;

/** Gênero (tag) do MangaDex, usado no filtro de busca (MNG-32). */
public record Genero(String id, String nome) {

    @Override
    public String toString() {
        return nome;
    }
}

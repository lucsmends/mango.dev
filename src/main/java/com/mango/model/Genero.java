package com.mango.model;

/**
 * Gênero (tag de grupo "genre" do MangaDex) usado como filtro de busca (UC1 / MNG-32).
 *
 * @param id   UUID da tag no MangaDex, usado em {@code includedTags[]}
 * @param nome nome localizado para exibição
 */
public record Genero(String id, String nome) {

    @Override
    public String toString() {
        return nome;
    }
}

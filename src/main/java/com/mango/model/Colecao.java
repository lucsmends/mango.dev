package com.mango.model;

/** Coleção nomeada da biblioteca pessoal (UC3). */
public record Colecao(long id, String nome, boolean removivel) {

    @Override
    public String toString() {
        return nome;
    }
}

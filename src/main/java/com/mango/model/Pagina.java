package com.mango.model;

/**
 * Uma pagina de um capitulo (UC2).
 *
 * @param numero numero da pagina (1-based)
 * @param url    URL da imagem da pagina no servidor at-home do MangaDex
 */
public record Pagina(int numero, String url) {
}

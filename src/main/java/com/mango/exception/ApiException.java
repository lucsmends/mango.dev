package com.mango.exception;

/** Falhas de comunicação com a API do MangaDex (EX1–EX3 do UC1, EX2 do UC2). */
public class ApiException extends MangoException {

    public ApiException(final String mensagem) {
        super(mensagem);
    }

    public ApiException(final String mensagem, final Throwable causa) {
        super(mensagem, causa);
    }
}

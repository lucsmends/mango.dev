package com.mango.exception;

/** Exceção base do domínio Mango . */
public class MangoException extends RuntimeException {

    public MangoException(final String mensagem) {
        super(mensagem);
    }

    public MangoException(final String mensagem, final Throwable causa) {
        super(mensagem, causa);
    }
}

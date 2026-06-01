package com.mango.exception;

/**
 * Exceção base do domínio Mango. Toda exceção de regra de negócio, persistência
 * ou integração externa deriva desta classe — nunca se captura {@code Exception}
 * genérica nas camadas de serviço.
 */
public class MangoException extends RuntimeException {

    public MangoException(final String mensagem) {
        super(mensagem);
    }

    public MangoException(final String mensagem, final Throwable causa) {
        super(mensagem, causa);
    }
}

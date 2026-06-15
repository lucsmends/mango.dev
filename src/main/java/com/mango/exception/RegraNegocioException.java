package com.mango.exception;

/** Violação de regra de negócio (ex.: RN1.1 — termo de busca mínimo). */
public class RegraNegocioException extends MangoException {

    public RegraNegocioException(final String mensagem) {
        super(mensagem);
    }
}

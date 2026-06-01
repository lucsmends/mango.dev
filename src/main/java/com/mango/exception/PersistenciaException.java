package com.mango.exception;

/**
 * Falha de acesso ao banco H2 local (conexão, escrita, leitura).
 */
public class PersistenciaException extends MangoException {

    public PersistenciaException(final String mensagem, final Throwable causa) {
        super(mensagem, causa);
    }
}

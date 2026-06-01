package com.mango.exception;

/**
 * Falha na comunicação com a API do MangaDex (sem conexão, 5xx persistente,
 * rate limit, resposta inválida). Mapeia os fluxos de exceção EX1–EX3 do UC1.
 */
public class MangaDexException extends MangoException {

    public MangaDexException(final String mensagem) {
        super(mensagem);
    }

    public MangaDexException(final String mensagem, final Throwable causa) {
        super(mensagem, causa);
    }
}

package com.mango.exception;

/**
 * Violação de uma regra de negócio do domínio (ex.: termo de busca inválido,
 * RN1.1). Tratada pelo Controller para exibir feedback claro ao Leitor.
 */
public class RegraNegocioException extends MangoException {

    public RegraNegocioException(final String mensagem) {
        super(mensagem);
    }
}

package com.mango.net;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Abstração mínima de "GET que devolve JSON".
 *
 * <p>Os serviços dependem desta interface (e não do cliente HTTP concreto),
 * o que permite testar todas as regras de negócio sem rede e sem mocks
 * pesados — basta um lambda devolvendo JSON fixo.</p>
 */
@FunctionalInterface
public interface JsonFetcher {

    JsonNode get(String url);
}

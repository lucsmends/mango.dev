package com.mango.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.mango.exception.MangaDexException;
import com.mango.util.HttpJsonClient.RespostaHttp;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa a lógica de retry/backoff do {@link HttpJsonClient} sem rede e sem
 * dormir de verdade, usando o transporte e o sleeper injetáveis.
 *
 * <p>Cobre RN1.4 (retry em 5xx), RN1.5 (429 com teto) e EX1 (sem conexão).</p>
 */
class HttpJsonClientTest {

    /** Sleeper no-op: não espera de verdade nos testes. */
    private static final HttpJsonClient.Sleeper SEM_ESPERA = millis -> { };

    @Test
    void retornaJsonNaPrimeiraTentativa() {
        final HttpJsonClient client = new HttpJsonClient(
                req -> new RespostaHttp(200, "{\"ok\":true}", Optional.empty()), SEM_ESPERA);

        final JsonNode json = client.getJson("https://x");
        assertTrue(json.path("ok").asBoolean());
    }

    @Test
    void repete429AteSucesso() {
        final AtomicInteger chamadas = new AtomicInteger();
        final HttpJsonClient client = new HttpJsonClient(req -> {
            final int n = chamadas.incrementAndGet();
            return (n <= 2)
                    ? new RespostaHttp(429, "", Optional.of("0"))      // RN1.5
                    : new RespostaHttp(200, "{\"ok\":true}", Optional.empty());
        }, SEM_ESPERA);

        final JsonNode json = client.getJson("https://x");
        assertTrue(json.path("ok").asBoolean());
        assertEquals(3, chamadas.get());
    }

    @Test
    void desisteApos429Excessivo() {
        final HttpJsonClient client = new HttpJsonClient(
                req -> new RespostaHttp(429, "", Optional.of("0")), SEM_ESPERA);

        final MangaDexException ex = assertThrows(MangaDexException.class,
                () -> client.getJson("https://x"));
        assertTrue(ex.getMessage().contains("Muitas requisições"));
    }

    @Test
    void repete5xxEDepoisFalhaComMensagemDeServidor() {
        final AtomicInteger chamadas = new AtomicInteger();
        final HttpJsonClient client = new HttpJsonClient(req -> {
            chamadas.incrementAndGet();
            return new RespostaHttp(503, "", Optional.empty());
        }, SEM_ESPERA);

        final MangaDexException ex = assertThrows(MangaDexException.class,
                () -> client.getJson("https://x"));
        assertTrue(ex.getMessage().contains("indisponível"));
        assertEquals(3, chamadas.get());   // RN1.4: 3 tentativas
    }

    @Test
    void falhaDeRedeViraMensagemDeSemConexao() {
        final HttpJsonClient client = new HttpJsonClient(req -> {
            throw new IOException("connection refused");
        }, SEM_ESPERA);

        final MangaDexException ex = assertThrows(MangaDexException.class,
                () -> client.getJson("https://x"));
        assertTrue(ex.getMessage().contains("Sem conexão"));   // EX1
    }
}

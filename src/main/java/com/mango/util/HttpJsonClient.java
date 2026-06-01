package com.mango.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.exception.MangaDexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP/JSON reutilizável sobre o {@link HttpClient} nativo do JDK.
 *
 * <p>Implementa as regras transversais do consumo da API do MangaDex:</p>
 * <ul>
 *   <li>RN1.4 — até {@value AppConfig#MAX_TENTATIVAS} tentativas em erro 5xx,
 *       com backoff exponencial (1s, 2s, 4s);</li>
 *   <li>RN1.5 — em HTTP 429, respeita o cabeçalho {@code Retry-After};</li>
 *   <li>EX1 — falha de conectividade vira {@link MangaDexException} com mensagem amigável.</li>
 * </ul>
 */
public final class HttpJsonClient {

    private static final Logger log = LoggerFactory.getLogger(HttpJsonClient.class);

    private final HttpClient http;
    private final ObjectMapper mapper;

    public HttpJsonClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.mapper = new ObjectMapper();
    }

    /**
     * Executa um GET e devolve o corpo como árvore JSON.
     *
     * @param url URL absoluta já codificada
     * @return o JSON raiz da resposta
     * @throws MangaDexException em falha de rede ou erro persistente da API
     */
    public JsonNode getJson(final String url) {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", AppConfig.USER_AGENT)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        int tentativa = 0;
        while (true) {
            tentativa++;
            try {
                final HttpResponse<String> resp =
                        http.send(request, HttpResponse.BodyHandlers.ofString());
                final int status = resp.statusCode();

                if (status >= 200 && status < 300) {
                    return mapper.readTree(resp.body());
                }
                if (status == 429) {                       // RN1.5
                    final long espera = retryAfterSegundos(resp);
                    log.warn("Rate limit (429). Aguardando {}s antes de nova tentativa.", espera);
                    dormir(espera * 1000L);
                    continue;
                }
                if (status >= 500 && tentativa < AppConfig.MAX_TENTATIVAS) {  // RN1.4
                    final long backoff = (long) Math.pow(2, tentativa - 1) * 1000L;
                    log.warn("Erro {} da API (tentativa {}/{}). Backoff {}ms.",
                            status, tentativa, AppConfig.MAX_TENTATIVAS, backoff);
                    dormir(backoff);
                    continue;
                }
                throw new MangaDexException("A API do MangaDex retornou HTTP " + status + ".");

            } catch (final IOException e) {
                if (tentativa < AppConfig.MAX_TENTATIVAS) {
                    final long backoff = (long) Math.pow(2, tentativa - 1) * 1000L;
                    log.warn("Falha de rede (tentativa {}/{}): {}. Backoff {}ms.",
                            tentativa, AppConfig.MAX_TENTATIVAS, e.getMessage(), backoff);
                    dormir(backoff);
                    continue;
                }
                throw new MangaDexException(
                        "Sem conexão. Verifique sua Internet e tente novamente.", e);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MangaDexException("Requisição interrompida.", e);
            }
        }
    }

    private long retryAfterSegundos(final HttpResponse<String> resp) {
        return resp.headers().firstValue("Retry-After")
                .map(v -> {
                    try {
                        return Long.parseLong(v.trim());
                    } catch (final NumberFormatException e) {
                        return 5L;
                    }
                })
                .orElse(5L);
    }

    private void dormir(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MangaDexException("Espera interrompida.", e);
        }
    }
}

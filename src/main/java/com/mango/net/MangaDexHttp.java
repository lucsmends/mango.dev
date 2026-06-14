package com.mango.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.config.Config;
import com.mango.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP da API MangaDex.
 *
 * <p>Implementa as regras de resiliência do UC1:</p>
 * <ul>
 *   <li><b>RN1.4</b> — até 3 tentativas em erro 5xx, com backoff 1s/2s/4s (EX2);</li>
 *   <li><b>RN1.5</b> — em HTTP 429 respeita o cabeçalho {@code Retry-After} (EX3);</li>
 *   <li><b>EX1</b> — sem conectividade, falha com mensagem amigável.</li>
 * </ul>
 */
public final class MangaDexHttp implements JsonFetcher {

    private static final Logger log = LoggerFactory.getLogger(MangaDexHttp.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Espera padrão quando o 429 não traz Retry-After. */
    private static final long RETRY_AFTER_PADRAO_S = 2;

    private final HttpClient client;

    public MangaDexHttp() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public JsonNode get(final String url) {
        long backoffMs = 1_000;                       // RN1.4: 1s, 2s, 4s
        for (int tentativa = 1; ; tentativa++) {
            final HttpResponse<String> resp = executar(url);
            final int status = resp.statusCode();

            if (status >= 200 && status < 300) {
                return parse(resp.body());
            }

            if (status == 429) {                      // RN1.5 / EX3
                final long esperaS = resp.headers()
                        .firstValueAsLong("Retry-After")
                        .orElse(RETRY_AFTER_PADRAO_S);
                log.warn("Rate limit (429). Aguardando {}s antes de repetir.", esperaS);
                dormir(esperaS * 1_000);
                continue;                             // 429 não consome tentativa de 5xx
            }

            if (status >= 500 && tentativa < Config.MAX_TENTATIVAS) {   // RN1.4 / EX2
                log.warn("API retornou {} (tentativa {}/{}). Backoff {}ms.",
                        status, tentativa, Config.MAX_TENTATIVAS, backoffMs);
                dormir(backoffMs);
                backoffMs *= 2;
                continue;
            }

            throw new ApiException("A API do MangaDex está indisponível (HTTP "
                    + status + "). Tente novamente mais tarde.");
        }
    }

    private HttpResponse<String> executar(final String url) {
        final HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", Config.USER_AGENT)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        try {
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (final IOException e) {               // EX1
            throw new ApiException("Sem conexão. Verifique sua Internet e tente novamente.", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Requisição interrompida.", e);
        }
    }

    private static JsonNode parse(final String body) {
        try {
            return MAPPER.readTree(body);
        } catch (final IOException e) {
            throw new ApiException("Resposta inválida da API do MangaDex.", e);
        }
    }

    private static void dormir(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Espera interrompida.", e);
        }
    }
}

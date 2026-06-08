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
import java.util.Optional;

/**
 * Cliente HTTP/JSON reutilizável sobre o {@link HttpClient} nativo do JDK.
 *
 * <p>Implementa as regras transversais do consumo da API do MangaDex:</p>
 * <ul>
 *   <li>RN1.4 — até {@value AppConfig#MAX_TENTATIVAS} tentativas em erro 5xx,
 *       com backoff exponencial (1s, 2s, 4s);</li>
 *   <li>RN1.5 — em HTTP 429, respeita o cabeçalho {@code Retry-After}, com teto
 *       de {@value #MAX_TENTATIVAS_429} tentativas para evitar espera infinita;</li>
 *   <li>EX1 — falha de conectividade vira {@link MangaDexException} com mensagem amigável;</li>
 *   <li>EX2 — 5xx persistente vira mensagem específica de servidor indisponível.</li>
 * </ul>
 *
 * <p>O transporte e a espera são injetáveis (construtor de pacote) para permitir
 * testes determinísticos da lógica de retry sem rede e sem dormir de verdade.</p>
 */
public final class HttpJsonClient {

    private static final Logger log = LoggerFactory.getLogger(HttpJsonClient.class);

    /** Teto de tentativas em resposta 429 antes de desistir. */
    public static final int MAX_TENTATIVAS_429 = 3;

    /** Resposta simplificada — desacopla a lógica de retry da API do JDK. */
    public record RespostaHttp(int status, String corpo, Optional<String> retryAfter) {
    }

    /** Camada de transporte (envio do request). Substituível em testes. */
    @FunctionalInterface
    public interface Transporte {
        RespostaHttp enviar(HttpRequest req) throws IOException, InterruptedException;
    }

    /** Abstração da espera entre tentativas. Substituível em testes (no-op). */
    @FunctionalInterface
    public interface Sleeper {
        void dormir(long millis) throws InterruptedException;
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final Transporte transporte;
    private final Sleeper sleeper;

    public HttpJsonClient() {
        final HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.transporte = req -> {
            final HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return new RespostaHttp(resp.statusCode(), resp.body(),
                    resp.headers().firstValue("Retry-After"));
        };
        this.sleeper = Thread::sleep;
    }

    /** Construtor para testes: injeta transporte e espera. */
    HttpJsonClient(final Transporte transporte, final Sleeper sleeper) {
        this.transporte = transporte;
        this.sleeper = sleeper;
    }

    /**
     * Executa um GET e devolve o corpo como árvore JSON, aplicando as políticas
     * de retry/backoff (RN1.4), rate limit (RN1.5) e mensagens de EX1/EX2.
     *
     * @throws MangaDexException em falha de rede, 5xx persistente, 429 excessivo
     *                           ou resposta inválida
     */
    public JsonNode getJson(final String url) {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", AppConfig.USER_AGENT)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        int tentativas5xx = 0;
        int tentativas429 = 0;
        int tentativasRede = 0;

        while (true) {
            try {
                final RespostaHttp resp = transporte.enviar(request);
                final int status = resp.status();

                if (status >= 200 && status < 300) {
                    return parse(resp.corpo());
                }
                if (status == 429) {                                   // RN1.5
                    if (++tentativas429 > MAX_TENTATIVAS_429) {
                        throw new MangaDexException(
                                "Muitas requisições à API do MangaDex (429). Tente novamente em instantes.");
                    }
                    final long espera = retryAfterSegundos(resp.retryAfter());
                    log.warn("Rate limit (429). Aguardando {}s (tentativa {}/{}).",
                            espera, tentativas429, MAX_TENTATIVAS_429);
                    dormir(espera * 1000L);
                    continue;
                }
                if (status >= 500) {                                   // RN1.4 / EX2
                    if (++tentativas5xx >= AppConfig.MAX_TENTATIVAS) {
                        throw new MangaDexException("O servidor do MangaDex está indisponível (HTTP "
                                + status + "). Tente novamente mais tarde.");
                    }
                    final long backoff = (long) Math.pow(2, tentativas5xx - 1) * 1000L;
                    log.warn("Erro {} da API (tentativa {}/{}). Backoff {}ms.",
                            status, tentativas5xx, AppConfig.MAX_TENTATIVAS, backoff);
                    dormir(backoff);
                    continue;
                }
                // Demais 4xx: erro definitivo, não adianta repetir.
                throw new MangaDexException("A API do MangaDex retornou HTTP " + status + ".");

            } catch (final IOException e) {                             // EX1
                if (++tentativasRede >= AppConfig.MAX_TENTATIVAS) {
                    throw new MangaDexException(
                            "Sem conexão. Verifique sua Internet e tente novamente.", e);
                }
                final long backoff = (long) Math.pow(2, tentativasRede - 1) * 1000L;
                log.warn("Falha de rede (tentativa {}/{}): {}. Backoff {}ms.",
                        tentativasRede, AppConfig.MAX_TENTATIVAS, e.getMessage(), backoff);
                dormir(backoff);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MangaDexException("Requisição interrompida.", e);
            }
        }
    }

    private JsonNode parse(final String corpo) {
        try {
            return mapper.readTree(corpo);
        } catch (final IOException e) {
            // Erro de parsing não é falha de rede: não deve disparar retry.
            throw new MangaDexException("Resposta inválida da API do MangaDex.", e);
        }
    }

    private long retryAfterSegundos(final Optional<String> header) {
        return header.map(v -> {
            try {
                return Long.parseLong(v.trim());
            } catch (final NumberFormatException e) {
                return 5L;
            }
        }).orElse(5L);
    }

    private void dormir(final long millis) {
        try {
            sleeper.dormir(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MangaDexException("Espera interrompida.", e);
        }
    }
}

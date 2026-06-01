package com.mango.util;

import java.nio.file.Path;

/**
 * Configurações centrais da aplicação Mango.
 *
 * <p>Concentra caminhos, parâmetros da API do MangaDex e constantes de regras
 * de negócio do UC1, evitando "magic numbers" espalhados pelo código.</p>
 */
public final class AppConfig {

    /** Diretório de dados do usuário (banco H2, cache de imagens, logs). */
    public static final Path DATA_DIR =
            Path.of(System.getProperty("user.home"), ".mango");

    /** URL JDBC do H2 em modo arquivo. AUTO_SERVER permite múltiplas conexões. */
    public static final String JDBC_URL =
            "jdbc:h2:" + DATA_DIR.resolve("mango") + ";AUTO_SERVER=TRUE";
    public static final String JDBC_USER = "sa";
    public static final String JDBC_PASS = "";

    /** Base da API REST do MangaDex. */
    public static final String API_BASE = "https://api.mangadex.org";

    /** Base para montar as URLs das capas. */
    public static final String COVER_BASE = "https://uploads.mangadex.org/covers";

    /** User-Agent recomendado pela documentação do MangaDex. */
    public static final String USER_AGENT = "Mango/1.0 (projeto-academico-unisinos)";

    /** Idioma preferido para capítulos e textos (RN1.6). */
    public static final String IDIOMA_PREFERIDO = "pt-br";
    /** Idioma de fallback quando o preferido não está disponível (RN1.6). */
    public static final String IDIOMA_FALLBACK = "en";

    /** Tamanho da página de resultados (RN1.3). */
    public static final int TAMANHO_PAGINA = 20;

    /** Termo de busca mínimo em caracteres não-brancos (RN1.1). */
    public static final int TERMO_BUSCA_MINIMO = 2;

    /** TTL do cache de buscas em minutos (RN1.2). */
    public static final int CACHE_BUSCA_TTL_MINUTOS = 30;

    /** Número máximo de tentativas em erro 5xx (RN1.4). */
    public static final int MAX_TENTATIVAS = 3;

    private AppConfig() {
    }
}

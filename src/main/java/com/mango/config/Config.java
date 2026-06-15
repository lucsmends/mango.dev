package com.mango.config;

import java.nio.file.Path;

/**
 * Configurações centrais.
 *
 * <p>Os dados ficam em {@code ~/.mango} de propósito, para que esta
 * versão possa ser testada lado a lado com a original sem disputar o banco.</p>
 */
public final class Config {

    /** Diretório de dados desta versão (isolado da versão original). */
    public static final Path DATA_DIR =
            Path.of(System.getProperty("user.home"), ".mango");

    public static final String JDBC_URL =
            "jdbc:h2:" + DATA_DIR.resolve("mango") + ";AUTO_SERVER=TRUE";
    public static final String JDBC_USER = "sa";
    public static final String JDBC_PASS = "";

    public static final String API_BASE = "https://api.mangadex.org";
    public static final String COVER_BASE = "https://uploads.mangadex.org/covers";
    public static final String USER_AGENT = "Mango/2.0 (projeto-academico-unisinos)";

    /** RN1.6 — idioma preferido e fallback. */
    public static final String IDIOMA_PREFERIDO = "pt-br";
    public static final String IDIOMA_FALLBACK = "en";

    /** RN1.3 — máximo de 20 mangás por página. */
    public static final int TAMANHO_PAGINA = 20;

    /** RN1.1 — mínimo de caracteres não-brancos no termo de busca. */
    public static final int TERMO_MINIMO = 2;

    /** RN1.2 — TTL do cache de buscas, em minutos. */
    public static final int CACHE_TTL_MINUTOS = 30;

    /** RN1.4 — tentativas em erro 5xx (backoff 1s, 2s, 4s). */
    public static final int MAX_TENTATIVAS = 3;

    /** RN2.1 — páginas pré-carregadas à frente da atual. */
    public static final int PRE_CARGA = 2;

    /** RN2.6 — limites de zoom do leitor. */
    public static final double ZOOM_MIN = 0.25;
    public static final double ZOOM_MAX = 4.0;

    private Config() {
    }
}

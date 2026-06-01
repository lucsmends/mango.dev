package com.mango.repository;

import com.mango.exception.PersistenciaException;
import com.mango.util.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Ponto único de acesso ao banco H2 local.
 *
 * <p>Responsável por garantir a existência do diretório de dados, abrir conexões
 * JDBC e executar o schema inicial de forma idempotente. Mantém-se simples
 * (sem pool de conexões) por ser uma aplicação desktop single-user; um pool
 * como HikariCP pode ser adicionado depois sem impacto nos repositórios.</p>
 */
public final class Database {

    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private static final String SCHEMA_RESOURCE = "/db/migration/V1__schema_inicial.sql";

    private Database() {
    }

    /**
     * Inicializa o banco: cria o diretório de dados e aplica o schema.
     * Deve ser chamado uma vez no bootstrap da aplicação.
     */
    public static void inicializar() {
        try {
            Files.createDirectories(AppConfig.DATA_DIR);
        } catch (final IOException e) {
            throw new PersistenciaException("Falha ao criar o diretório de dados " + AppConfig.DATA_DIR, e);
        }
        aplicarSchema();
        log.info("Banco H2 pronto em {}", AppConfig.DATA_DIR.resolve("mango"));
    }

    /** Abre uma nova conexão JDBC. O chamador é responsável por fechá-la. */
    public static Connection conexao() {
        try {
            return DriverManager.getConnection(AppConfig.JDBC_URL, AppConfig.JDBC_USER, AppConfig.JDBC_PASS);
        } catch (final SQLException e) {
            throw new PersistenciaException("Não foi possível conectar ao banco local", e);
        }
    }

    private static void aplicarSchema() {
        final String script = lerRecurso();
        try (Connection conn = conexao(); Statement stmt = conn.createStatement()) {
            for (final String comando : script.split(";")) {
                final String sql = comando.strip();
                if (!sql.isEmpty()) {
                    stmt.execute(sql);
                }
            }
        } catch (final SQLException e) {
            throw new PersistenciaException("Falha ao aplicar o schema inicial", e);
        }
    }

    private static String lerRecurso() {
        try (InputStream in = Database.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new PersistenciaException("Recurso de schema não encontrado: " + SCHEMA_RESOURCE, null);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (final IOException e) {
            throw new PersistenciaException("Falha ao ler o schema inicial", e);
        }
    }
}

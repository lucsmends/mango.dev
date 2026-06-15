package com.mango.db;

import com.mango.config.Config;
import com.mango.exception.MangoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Conexão H2 + aplicação idempotente do schema.
 *
 * <p>O banco fica em {@code ~/.mango}, isolado da versão original.</p>
 */
public final class Database {

    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private static volatile boolean inicializado;

    private Database() {
    }

    public static Connection conectar() {
        try {
            return DriverManager.getConnection(
                    Config.JDBC_URL, Config.JDBC_USER, Config.JDBC_PASS);
        } catch (final SQLException e) {
            throw new MangoException("Não foi possível abrir o banco local.", e);
        }
    }

    /** Cria o diretório de dados e aplica o schema (CREATE IF NOT EXISTS). */
    public static synchronized void inicializar() {
        if (inicializado) {
            return;
        }
        try {
            Files.createDirectories(Config.DATA_DIR);
        } catch (final IOException e) {
            throw new MangoException("Não foi possível criar " + Config.DATA_DIR, e);
        }
        try (Connection con = conectar(); Statement st = con.createStatement()) {
            st.execute(lerSchema());
            inicializado = true;
            log.info("Banco inicializado em {}", Config.DATA_DIR);
        } catch (final SQLException e) {
            throw new MangoException("Falha ao aplicar o schema do banco.", e);
        }
    }

    private static String lerSchema() {
        try (var in = Database.class.getResourceAsStream("/db/schema.sql")) {
            if (in == null) {
                throw new MangoException("schema.sql não encontrado no classpath.");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new MangoException("Falha ao ler schema.sql.", e);
        }
    }
}

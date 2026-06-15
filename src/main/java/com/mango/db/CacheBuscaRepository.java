package com.mango.db;

import com.mango.config.Config;
import com.mango.exception.MangoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Cache de buscas em H2 (RN1.2): guarda o JSON bruto da API por chave de
 * filtros, com TTL de 30 minutos validado na leitura.
 */
public class CacheBuscaRepository {

    private static final Logger log = LoggerFactory.getLogger(CacheBuscaRepository.class);

    /** Payload ainda válido para a chave, se existir (FA2). */
    public Optional<String> buscar(final String chave) {
        final String sql = "SELECT payload, criado_em FROM cache_busca WHERE chave = ?";
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, chave);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                final Instant criadoEm = rs.getTimestamp("criado_em").toInstant();
                final Instant limite = Instant.now()
                        .minus(Config.CACHE_TTL_MINUTOS, ChronoUnit.MINUTES);
                if (criadoEm.isBefore(limite)) {
                    return Optional.empty();          // expirado (RN1.2)
                }
                return Optional.of(rs.getString("payload"));
            }
        } catch (final SQLException e) {
            // Cache nunca derruba a busca: loga e segue para a API.
            log.warn("Falha ao ler cache de busca; ignorando.", e);
            return Optional.empty();
        }
    }

    public void salvar(final String chave, final String payload) {
        final String sql = """
                MERGE INTO cache_busca (chave, payload, criado_em)
                KEY (chave) VALUES (?, ?, ?)""";
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, chave);
            ps.setString(2, payload);
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new MangoException("Falha ao gravar cache de busca.", e);
        }
    }
}

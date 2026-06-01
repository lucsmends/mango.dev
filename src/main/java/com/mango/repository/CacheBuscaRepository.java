package com.mango.repository;

import com.mango.exception.PersistenciaException;
import com.mango.util.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cache local de buscas do catálogo em H2 (RN1.2).
 *
 * <p>Guarda o payload JSON bruto da resposta da API indexado por uma chave de
 * filtros, com TTL de {@value AppConfig#CACHE_BUSCA_TTL_MINUTOS} minutos validado
 * em leitura. Entradas expiradas são ignoradas (e sobrescritas na próxima escrita).</p>
 */
public final class CacheBuscaRepository {

    private static final Logger log = LoggerFactory.getLogger(CacheBuscaRepository.class);

    /** Retorna o payload em cache se existir e estiver dentro do TTL (FA2). */
    public Optional<String> buscar(final String chave) {
        final String sql = "SELECT payload_json, criado_em FROM cache_busca WHERE chave_filtros = ?";
        try (Connection conn = Database.conexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chave);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final LocalDateTime criadoEm = rs.getTimestamp("criado_em").toLocalDateTime();
                    final LocalDateTime expira = criadoEm.plusMinutes(AppConfig.CACHE_BUSCA_TTL_MINUTOS);
                    if (LocalDateTime.now().isBefore(expira)) {
                        return Optional.of(rs.getString("payload_json"));
                    }
                    log.debug("Cache expirado para a chave {}", chave);
                }
            }
            return Optional.empty();
        } catch (final Exception e) {
            // Cache é otimização: falha de leitura não deve quebrar a busca.
            log.warn("Falha ao ler cache de busca: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Grava (ou atualiza) o payload da busca para a chave informada. */
    public void salvar(final String chave, final String payloadJson) {
        final String sql = """
                MERGE INTO cache_busca (chave_filtros, payload_json, criado_em)
                KEY(chave_filtros) VALUES (?, ?, ?)
                """;
        try (Connection conn = Database.conexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chave);
            ps.setString(2, payloadJson);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (final Exception e) {
            // Idem: não impedir a navegação por falha de escrita no cache.
            log.warn("Falha ao gravar cache de busca: {}", e.getMessage());
        }
    }
}

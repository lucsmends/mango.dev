package com.mango.repository;

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
 * em leitura. Além da leitura "fresca" (FA2), expõe uma leitura que ignora o TTL,
 * usada como degradação graciosa quando a API está fora do ar (EX1/EX2).</p>
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
            log.warn("Falha ao ler cache de busca: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retorna o payload em cache independentemente do TTL. Usado como fallback
     * quando a API falha (EX1/EX2): é melhor mostrar dados levemente antigos do
     * que deixar o Leitor sem nada.
     */
    public Optional<String> buscarIgnorandoValidade(final String chave) {
        final String sql = "SELECT payload_json FROM cache_busca WHERE chave_filtros = ?";
        try (Connection conn = Database.conexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString("payload_json")) : Optional.empty();
            }
        } catch (final Exception e) {
            log.warn("Falha ao ler cache (fallback): {}", e.getMessage());
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
            log.warn("Falha ao gravar cache de busca: {}", e.getMessage());
        }
    }

    /**
     * Remove entradas de cache mais antigas que o TTL, evitando crescimento
     * indefinido da tabela. Seguro para chamar na inicialização.
     *
     * @return número de linhas removidas
     */
    public int removerExpirados() {
        final String sql = "DELETE FROM cache_busca WHERE criado_em < ?";
        final LocalDateTime limite = LocalDateTime.now().minusMinutes(AppConfig.CACHE_BUSCA_TTL_MINUTOS);
        try (Connection conn = Database.conexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(limite));
            final int n = ps.executeUpdate();
            if (n > 0) {
                log.debug("Cache de busca: {} entrada(s) expirada(s) removida(s).", n);
            }
            return n;
        } catch (final Exception e) {
            log.warn("Falha ao limpar cache expirado: {}", e.getMessage());
            return 0;
        }
    }
}

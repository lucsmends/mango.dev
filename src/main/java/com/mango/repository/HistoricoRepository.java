package com.mango.repository;

import com.mango.model.ProgressoLeitura;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Persistencia do progresso de leitura no H2 (tabela historico_leitura).
 * Atende RN2.2 (progresso salvo a cada mudanca), RN2.3 (capitulo concluido) e
 * FA5 (retomar do ultimo ponto).
 */
public final class HistoricoRepository {

    private static final Logger log = LoggerFactory.getLogger(HistoricoRepository.class);

    /** Le o progresso salvo de um capitulo, se existir. */
    public Optional<ProgressoLeitura> buscar(final String mangaId, final String capituloId) {
        final String sql = "SELECT pagina_atual, total_paginas, concluido "
                + "FROM historico_leitura WHERE manga_id = ? AND capitulo_id = ?";
        try (Connection conn = Database.conexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mangaId);
            ps.setString(2, capituloId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ProgressoLeitura(
                            rs.getInt("pagina_atual"),
                            rs.getInt("total_paginas"),
                            rs.getBoolean("concluido")));
                }
            }
            return Optional.empty();
        } catch (final Exception e) {
            log.warn("Falha ao ler progresso de leitura: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Insere ou atualiza o progresso de um capitulo (RN2.2). */
    public void salvar(final String mangaId, final String capituloId,
                       final int paginaAtual, final int totalPaginas, final boolean concluido) {
        final String sql = """
                MERGE INTO historico_leitura
                  (manga_id, capitulo_id, pagina_atual, total_paginas, concluido, atualizado_em)
                KEY(manga_id, capitulo_id) VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = Database.conexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mangaId);
            ps.setString(2, capituloId);
            ps.setInt(3, paginaAtual);
            ps.setInt(4, totalPaginas);
            ps.setBoolean(5, concluido);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (final Exception e) {
            log.warn("Falha ao salvar progresso de leitura: {}", e.getMessage());
        }
    }
}

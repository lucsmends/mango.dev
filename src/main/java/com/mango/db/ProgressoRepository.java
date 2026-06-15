package com.mango.db;

import com.mango.exception.MangoException;
import com.mango.model.Progresso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Persistência do progresso de leitura (RN2.2/RN2.3, FA5 do UC2). */
public class ProgressoRepository {

    public void salvar(final String mangaId, final String capituloId,
                       final int paginaAtual, final int totalPaginas,
                       final boolean concluido) {
        final String sql = """
                MERGE INTO progresso_leitura
                    (manga_id, capitulo_id, pagina_atual, total_paginas, concluido, atualizado_em)
                KEY (manga_id, capitulo_id)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)""";
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, mangaId);
            ps.setString(2, capituloId);
            ps.setInt(3, paginaAtual);
            ps.setInt(4, totalPaginas);
            ps.setBoolean(5, concluido);
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new MangoException(
                    "Não foi possível salvar a alteração. Verifique o espaço em disco.", e);
        }
    }

    public Optional<Progresso> buscar(final String mangaId, final String capituloId) {
        final String sql = """
                SELECT pagina_atual, total_paginas, concluido
                  FROM progresso_leitura
                 WHERE manga_id = ? AND capitulo_id = ?""";
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, mangaId);
            ps.setString(2, capituloId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Progresso(
                        rs.getInt("pagina_atual"),
                        rs.getInt("total_paginas"),
                        rs.getBoolean("concluido")));
            }
        } catch (final SQLException e) {
            throw new MangoException("Falha ao consultar o progresso de leitura.", e);
        }
    }
}

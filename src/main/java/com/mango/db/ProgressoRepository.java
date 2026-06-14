package com.mango.db;

import com.mango.exception.MangoException;
import com.mango.model.LeituraRecente;
import com.mango.model.Progresso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persistência do progresso de leitura (RN2.2/RN2.3, FA5; histórico do UC3). */
public class ProgressoRepository {

    private static final String COLUNAS =
            "manga_id, capitulo_id, titulo, capa_url, cap_numero, "
            + "pagina_atual, total_paginas, concluido";

    public void salvar(final String mangaId, final String capituloId,
                       final String titulo, final String capaUrl, final String capNumero,
                       final int paginaAtual, final int totalPaginas, final boolean concluido) {
        final String sql = """
                MERGE INTO progresso_leitura
                    (manga_id, capitulo_id, titulo, capa_url, cap_numero,
                     pagina_atual, total_paginas, concluido, atualizado_em)
                KEY (manga_id, capitulo_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)""";
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, mangaId);
            ps.setString(2, capituloId);
            ps.setString(3, titulo);
            ps.setString(4, capaUrl);
            ps.setString(5, capNumero);
            ps.setInt(6, paginaAtual);
            ps.setInt(7, totalPaginas);
            ps.setBoolean(8, concluido);
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

    /** Histórico completo, mais recentes primeiro (RN3.5). */
    public List<LeituraRecente> listarHistorico() {
        return consultar("SELECT " + COLUNAS
                + " FROM progresso_leitura ORDER BY atualizado_em DESC");
    }

    /** Capítulos em andamento (não concluídos), para "Continuar lendo". */
    public List<LeituraRecente> listarEmAndamento() {
        return consultar("SELECT " + COLUNAS
                + " FROM progresso_leitura WHERE concluido = FALSE AND total_paginas > 0"
                + " ORDER BY atualizado_em DESC");
    }

    private List<LeituraRecente> consultar(final String sql) {
        final List<LeituraRecente> out = new ArrayList<>();
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new LeituraRecente(
                        rs.getString("manga_id"),
                        rs.getString("capitulo_id"),
                        rs.getString("titulo"),
                        rs.getString("capa_url"),
                        rs.getString("cap_numero"),
                        rs.getInt("pagina_atual"),
                        rs.getInt("total_paginas"),
                        rs.getBoolean("concluido")));
            }
            return out;
        } catch (final SQLException e) {
            throw new MangoException("Falha ao consultar o histórico de leitura.", e);
        }
    }
}

package com.mango.db;

import com.mango.exception.MangoException;
import com.mango.model.RegistroLeitura;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Leitura de dados para os relatórios do UC3 (apenas consultas). */
public class RelatorioRepository {

    /**
     * Registros de leitura no intervalo informado (limites opcionais).
     *
     * @param de  início do período (inclusive) ou {@code null} para sem limite
     * @param ate fim do período (inclusive) ou {@code null} para sem limite
     */
    public List<RegistroLeitura> leituras(final Instant de, final Instant ate) {
        final StringBuilder sql = new StringBuilder(
                "SELECT manga_id, titulo, pagina_atual, total_paginas, concluido "
                + "FROM progresso_leitura WHERE titulo IS NOT NULL");
        if (de != null) {
            sql.append(" AND atualizado_em >= ?");
        }
        if (ate != null) {
            sql.append(" AND atualizado_em <= ?");
        }

        final List<RegistroLeitura> out = new ArrayList<>();
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int i = 1;
            if (de != null) {
                ps.setTimestamp(i++, Timestamp.from(de));
            }
            if (ate != null) {
                ps.setTimestamp(i, Timestamp.from(ate));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new RegistroLeitura(
                            rs.getString("manga_id"),
                            rs.getString("titulo"),
                            rs.getInt("pagina_atual"),
                            rs.getInt("total_paginas"),
                            rs.getBoolean("concluido")));
                }
            }
            return out;
        } catch (final SQLException e) {
            throw new MangoException("Falha ao consultar dados do relatório.", e);
        }
    }

    /** Gêneros de todos os itens da biblioteca, um por ocorrência (para ranking). */
    public List<String> generosBiblioteca() {
        final List<String> out = new ArrayList<>();
        final String sql = "SELECT generos FROM item_biblioteca WHERE generos IS NOT NULL";
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                for (final String g : rs.getString("generos").split(";")) {
                    if (!g.isBlank()) {
                        out.add(g.trim());
                    }
                }
            }
            return out;
        } catch (final SQLException e) {
            throw new MangoException("Falha ao consultar gêneros da biblioteca.", e);
        }
    }
}

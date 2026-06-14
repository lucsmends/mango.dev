package com.mango.db;

import com.mango.exception.MangoException;
import com.mango.model.Colecao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Persistência das coleções da biblioteca (UC3). */
public class ColecaoRepository {

    public List<Colecao> listar() {
        final String sql = "SELECT id, nome, removivel FROM colecao ORDER BY removivel DESC, LOWER(nome)";
        final List<Colecao> out = new ArrayList<>();
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Colecao(rs.getLong("id"), rs.getString("nome"), rs.getBoolean("removivel")));
            }
            return out;
        } catch (final SQLException e) {
            throw new MangoException("Falha ao listar as coleções.", e);
        }
    }

    public boolean existeNome(final String nome) {
        final String sql = "SELECT 1 FROM colecao WHERE LOWER(nome) = LOWER(?)";
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (final SQLException e) {
            throw new MangoException("Falha ao verificar o nome da coleção.", e);
        }
    }

    public long criar(final String nome) {
        final String sql = "INSERT INTO colecao (nome, removivel) VALUES (?, TRUE)";
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (final SQLException e) {
            throw new MangoException("Falha ao criar a coleção.", e);
        }
    }

    /** Remove a coleção (apenas se for removível) e seus itens (ON DELETE CASCADE). */
    public void remover(final long id) {
        final String sql = "DELETE FROM colecao WHERE id = ? AND removivel = TRUE";
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new MangoException("Falha ao remover a coleção.", e);
        }
    }
}

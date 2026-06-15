package com.mango.db;

import com.mango.exception.MangoException;
import com.mango.model.ItemBiblioteca;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persistência dos mangás dentro das coleções (UC3, RN3.1). */
public class BibliotecaRepository {

    /** Adiciona (idempotente — RN3.1: sem duplicar na mesma coleção). */
    public void adicionar(final long colecaoId, final ItemBiblioteca item, final List<String> generos) {
        final String sql = """
                MERGE INTO item_biblioteca (colecao_id, manga_id, titulo, capa_url, generos)
                KEY (colecao_id, manga_id) VALUES (?, ?, ?, ?, ?)""";
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, colecaoId);
            ps.setString(2, item.mangaId());
            ps.setString(3, item.titulo());
            ps.setString(4, item.capaUrl());
            ps.setString(5, generos == null || generos.isEmpty() ? null : String.join(";", generos));
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new MangoException("Não foi possível salvar na biblioteca.", e);
        }
    }

    public void remover(final long colecaoId, final String mangaId) {
        final String sql = "DELETE FROM item_biblioteca WHERE colecao_id = ? AND manga_id = ?";
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, colecaoId);
            ps.setString(2, mangaId);
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new MangoException("Não foi possível remover da biblioteca.", e);
        }
    }

    public List<ItemBiblioteca> listarItens(final long colecaoId) {
        final String sql = """
                SELECT manga_id, titulo, capa_url FROM item_biblioteca
                 WHERE colecao_id = ? ORDER BY adicionado_em DESC""";
        final List<ItemBiblioteca> out = new ArrayList<>();
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, colecaoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ItemBiblioteca(
                            rs.getString("manga_id"),
                            rs.getString("titulo"),
                            rs.getString("capa_url")));
                }
            }
            return out;
        } catch (final SQLException e) {
            throw new MangoException("Falha ao carregar a coleção.", e);
        }
    }

    /** IDs das coleções que já contêm o mangá (para marcar no menu da ficha). */
    public Set<Long> colecoesDoManga(final String mangaId) {
        final String sql = "SELECT colecao_id FROM item_biblioteca WHERE manga_id = ?";
        final Set<Long> out = new HashSet<>();
        try (Connection con = Database.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, mangaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getLong(1));
                }
            }
            return out;
        } catch (final SQLException e) {
            throw new MangoException("Falha ao consultar a biblioteca.", e);
        }
    }
}

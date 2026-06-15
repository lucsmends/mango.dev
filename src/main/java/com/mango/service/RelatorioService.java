package com.mango.service;

import com.mango.db.RelatorioRepository;
import com.mango.exception.RegraNegocioException;
import com.mango.model.RegistroLeitura;
import com.mango.model.Relatorio;
import com.mango.model.Relatorio.Contagem;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Relatórios de leitura do UC3.
 *
 * <p>Cobre: estatísticas de capítulos/páginas/tempo (RN3.4 — 30s por página),
 * gêneros favoritos a partir da biblioteca, exportação CSV em UTF-8 com BOM
 * (RN3.6) e validação de período (EX2 — data inicial &gt; final).</p>
 */
public class RelatorioService {

    /** RN3.4 — segundos estimados por página efetivamente exibida. */
    private static final int SEGUNDOS_POR_PAGINA = 30;
    private static final int TOP_N = 5;
    /** RN3.6 — BOM UTF-8 para o CSV abrir corretamente no Excel. */
    private static final String BOM = "﻿";

    private final RelatorioRepository repo;

    public RelatorioService() {
        this(new RelatorioRepository());
    }

    /** Injeção para testes: o repositório pode ser um stub em memória. */
    public RelatorioService(final RelatorioRepository repo) {
        this.repo = repo;
    }

    /**
     * Gera o relatório no período informado (limites opcionais, {@code null} = sem limite).
     *
     * @throws RegraNegocioException se {@code de} for posterior a {@code ate} (EX2)
     */
    public Relatorio gerar(final Instant de, final Instant ate) {
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new RegraNegocioException("A data inicial não pode ser posterior à final.");
        }

        final List<RegistroLeitura> leituras = repo.leituras(de, ate);
        final Set<String> mangas = new HashSet<>();
        final Map<String, Long> paginasPorManga = new LinkedHashMap<>();
        int concluidos = 0;
        int emAndamento = 0;
        int paginas = 0;

        for (final RegistroLeitura r : leituras) {
            mangas.add(r.mangaId());
            final int lidas = r.paginasLidas();
            paginas += lidas;
            if (r.concluido()) {
                concluidos++;
            } else if (r.totalPaginas() > 0) {
                emAndamento++;
            }
            paginasPorManga.merge(tituloExibicao(r.titulo()), (long) lidas, Long::sum);
        }

        final long minutos = (long) paginas * SEGUNDOS_POR_PAGINA / 60;

        final Map<String, Long> generos = new LinkedHashMap<>();
        for (final String g : repo.generosBiblioteca()) {
            if (g != null && !g.isBlank()) {
                generos.merge(g, 1L, Long::sum);
            }
        }

        return new Relatorio(mangas.size(), concluidos, emAndamento, paginas, minutos,
                top(paginasPorManga), top(generos));
    }

    /** Exporta o relatório em CSV UTF-8 com BOM (RN3.6); separador ';' (Excel pt-BR). */
    public String exportarCsv(final Relatorio r) {
        final StringBuilder sb = new StringBuilder(BOM);
        sb.append("Métrica;Valor\n");
        linha(sb, "Mangás lidos", String.valueOf(r.mangasLidos()));
        linha(sb, "Capítulos concluídos", String.valueOf(r.capitulosConcluidos()));
        linha(sb, "Capítulos em andamento", String.valueOf(r.capitulosEmAndamento()));
        linha(sb, "Páginas lidas", String.valueOf(r.paginasLidas()));
        linha(sb, "Tempo estimado de leitura", r.tempoFormatado());

        sb.append('\n').append("Mangás mais lidos;Páginas\n");
        for (final Contagem c : r.topMangas()) {
            linha(sb, c.rotulo(), String.valueOf(c.valor()));
        }

        sb.append('\n').append("Gêneros favoritos;Ocorrências\n");
        for (final Contagem c : r.topGeneros()) {
            linha(sb, c.rotulo(), String.valueOf(c.valor()));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------

    private static List<Contagem> top(final Map<String, Long> contagens) {
        return contagens.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_N)
                .map(e -> new Contagem(e.getKey(), e.getValue()))
                .toList();
    }

    private static void linha(final StringBuilder sb, final String campo, final String valor) {
        sb.append(escapar(campo)).append(';').append(escapar(valor)).append('\n');
    }

    /** Envolve em aspas e duplica aspas internas quando há separador/aspas/quebra. */
    private static String escapar(final String s) {
        final String v = s == null ? "" : s;
        if (v.contains(";") || v.contains("\"") || v.contains("\n")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    private static String tituloExibicao(final String titulo) {
        return titulo == null || titulo.isBlank() ? "(sem título)" : titulo;
    }
}

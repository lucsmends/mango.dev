package com.mango.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mango.config.Config;
import com.mango.db.ProgressoRepository;
import com.mango.exception.ApiException;
import com.mango.model.Capitulo;
import com.mango.model.Manga;
import com.mango.model.Pagina;
import com.mango.model.Progresso;
import com.mango.net.JsonFetcher;
import com.mango.net.MangaDexHttp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serviço do leitor (UC2): resolve as URLs das páginas de um capítulo
 * (endpoint at-home) e persiste o progresso de leitura.
 */
public class LeitorService {

    private final JsonFetcher http;
    private final ProgressoRepository progresso;

    public LeitorService() {
        this(new MangaDexHttp(), new ProgressoRepository());
    }

    public LeitorService(final JsonFetcher http, final ProgressoRepository progresso) {
        this.http = http;
        this.progresso = progresso;
    }

    /**
     * URLs das páginas do capítulo.
     *
     * @throws ApiException se o capítulo não tiver páginas (EX2 do UC2)
     */
    public List<Pagina> paginas(final String capituloId) {
        final JsonNode root = http.get(Config.API_BASE + "/at-home/server/" + capituloId);
        final String base = root.path("baseUrl").asText("");
        final String hash = root.path("chapter").path("hash").asText("");

        final List<Pagina> paginas = new ArrayList<>();
        int n = 1;
        for (final JsonNode arquivo : root.path("chapter").path("data")) {
            paginas.add(new Pagina(n++, base + "/data/" + hash + "/" + arquivo.asText()));
        }
        if (paginas.isEmpty()) {
            throw new ApiException("Este capítulo ainda não está disponível para leitura.");
        }
        return paginas;
    }

    /**
     * RN2.2 — persiste a cada mudança de página; RN2.3 — concluído quando a
     * última página é atingida. {@code paginaAtual} é zero-based.
     */
    public void salvarProgresso(final Manga manga, final Capitulo capitulo,
                                final int paginaAtual, final int totalPaginas) {
        final boolean concluido = totalPaginas > 0 && paginaAtual >= totalPaginas - 1;
        progresso.salvar(manga.id(), capitulo.id(), manga.titulo(), manga.capaUrl(),
                capitulo.numero(), paginaAtual, totalPaginas, concluido);
    }

    /** FA5 — progresso salvo para retomar a leitura. */
    public Optional<Progresso> progressoSalvo(final String mangaId, final String capituloId) {
        return progresso.buscar(mangaId, capituloId);
    }
}

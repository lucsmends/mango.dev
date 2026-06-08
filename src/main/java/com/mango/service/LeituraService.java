package com.mango.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mango.exception.MangaDexException;
import com.mango.model.Pagina;
import com.mango.model.ProgressoLeitura;
import com.mango.repository.HistoricoRepository;
import com.mango.util.AppConfig;
import com.mango.util.HttpJsonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servico do leitor (UC2). Carrega as paginas de um capitulo via endpoint
 * at-home do MangaDex e registra o progresso de leitura.
 */
public final class LeituraService {

    private final HttpJsonClient http;
    private final HistoricoRepository historico;

    public LeituraService() {
        this(new HttpJsonClient(), new HistoricoRepository());
    }

    /** Construtor para testes (injeta dependencias). */
    public LeituraService(final HttpJsonClient http, final HistoricoRepository historico) {
        this.http = http;
        this.historico = historico;
    }

    /**
     * Carrega as URLs das paginas de um capitulo.
     *
     * @throws MangaDexException se o capitulo nao tiver paginas (EX2) ou a API falhar
     */
    public List<Pagina> carregarPaginas(final String capituloId) {
        final JsonNode root = http.getJson(AppConfig.API_BASE + "/at-home/server/" + capituloId);
        final String baseUrl = root.path("baseUrl").asText("");
        final JsonNode chapter = root.path("chapter");
        final String hash = chapter.path("hash").asText("");
        final JsonNode data = chapter.path("data");

        final List<Pagina> paginas = new ArrayList<>();
        int numero = 1;
        if (data.isArray()) {
            for (final JsonNode arquivo : data) {
                final String url = baseUrl + "/data/" + hash + "/" + arquivo.asText();
                paginas.add(new Pagina(numero++, url));
            }
        }
        if (paginas.isEmpty()) {
            throw new MangaDexException("Este capitulo ainda nao esta disponivel para leitura.");
        }
        return paginas;
    }

    /** Registra o progresso; marca como concluido ao atingir a ultima pagina (RN2.2/RN2.3). */
    public void registrarProgresso(final String mangaId, final String capituloId,
                                   final int paginaAtual, final int totalPaginas) {
        final boolean concluido = totalPaginas > 0 && paginaAtual >= totalPaginas - 1;
        historico.salvar(mangaId, capituloId, paginaAtual, totalPaginas, concluido);
    }

    /** Progresso salvo de um capitulo, para retomar a leitura (FA5). */
    public Optional<ProgressoLeitura> progresso(final String mangaId, final String capituloId) {
        return historico.buscar(mangaId, capituloId);
    }
}

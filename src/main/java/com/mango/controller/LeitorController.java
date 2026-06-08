package com.mango.controller;

import com.mango.model.Capitulo;
import com.mango.model.Manga;
import com.mango.model.Pagina;
import com.mango.model.ProgressoLeitura;
import com.mango.service.LeituraService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller do leitor de capitulos (UC2) - modo de pagina unica.
 *
 * Cobre: carregamento das paginas, navegacao por teclado e mouse, zoom (RN2.6),
 * pre-carga das proximas paginas (RN2.1), persistencia de progresso (RN2.2),
 * marcacao de concluido (RN2.3) e retomada do ultimo ponto lido (FA5).
 */
public class LeitorController {

    private static final Logger log = LoggerFactory.getLogger(LeitorController.class);

    private static final double ZOOM_MIN = 0.25;   // RN2.6
    private static final double ZOOM_MAX = 4.0;    // RN2.6
    private static final double ZOOM_PASSO = 0.25;
    private static final double LARGURA_BASE = 800;

    @FXML private BorderPane raiz;
    @FXML private Label lblTitulo;
    @FXML private Label lblInfo;
    @FXML private Label lblZoom;
    @FXML private ScrollPane scroll;
    @FXML private ImageView imgPagina;

    private LeituraService service;
    private Manga manga;
    private Capitulo capitulo;
    private Parent fichaRoot;
    private List<Pagina> paginas = List.of();
    private int indice;
    private double zoom = 1.0;

    private final Map<Integer, Image> cacheImagens = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        final Thread t = new Thread(r, "leitor");
        t.setDaemon(true);
        return t;
    });

    /** Abre o leitor para um capitulo. */
    public void carregar(final LeituraService service, final Manga manga,
                         final Capitulo capitulo, final Parent fichaRoot) {
        this.service = service;
        this.manga = manga;
        this.capitulo = capitulo;
        this.fichaRoot = fichaRoot;

        lblTitulo.setText(manga.titulo() + "  -  " + capitulo.rotulo());
        aplicarZoom();
        raiz.setOnKeyPressed(this::onKey);
        raiz.setFocusTraversable(true);
        Platform.runLater(raiz::requestFocus);
        lblInfo.setText("Carregando paginas...");

        final Task<List<Pagina>> task = new Task<>() {
            @Override
            protected List<Pagina> call() {
                return service.carregarPaginas(capitulo.id());
            }
        };
        task.setOnSucceeded(e -> {
            paginas = task.getValue();
            int inicio = 0;
            final Optional<ProgressoLeitura> p = service.progresso(manga.id(), capitulo.id());
            if (p.isPresent() && p.get().paginaAtual() > 0 && p.get().paginaAtual() < paginas.size()) {
                inicio = p.get().paginaAtual();                      // FA5
            }
            mostrar(inicio);
        });
        task.setOnFailed(e -> {
            final Throwable c = task.getException();
            lblInfo.setText(c != null ? c.getMessage() : "Falha ao carregar o capitulo.");
            log.warn("Falha ao carregar paginas de {}", capitulo.id(), c);
        });
        executor.submit(task);
    }

    private void mostrar(final int novoIndice) {
        if (paginas.isEmpty()) {
            return;
        }
        indice = Math.max(0, Math.min(novoIndice, paginas.size() - 1));
        imgPagina.setImage(imagem(indice));
        scroll.setVvalue(0);
        atualizarInfo();
        precarregar();                                               // RN2.1
        service.registrarProgresso(manga.id(), capitulo.id(), indice, paginas.size()); // RN2.2/RN2.3
    }

    private Image imagem(final int i) {
        return cacheImagens.computeIfAbsent(i, k -> new Image(paginas.get(k).url(), true));
    }

    private void precarregar() {
        for (int i = indice + 1; i <= indice + 2 && i < paginas.size(); i++) {
            imagem(i);
        }
    }

    private void proxima() {
        if (indice < paginas.size() - 1) {
            mostrar(indice + 1);
        }
    }

    private void anterior() {
        if (indice > 0) {
            mostrar(indice - 1);
        }
    }

    private void onKey(final KeyEvent e) {
        switch (e.getCode()) {
            case RIGHT, SPACE, DOWN, PAGE_DOWN -> proxima();
            case LEFT, UP, PAGE_UP -> anterior();
            case ADD, EQUALS -> zoomMais();
            case SUBTRACT, MINUS -> zoomMenos();
            case F11 -> alternarTelaCheia();
            case ESCAPE -> onVoltar();
            case HOME -> mostrar(0);
            case END -> mostrar(paginas.size() - 1);
            default -> {
                return;
            }
        }
        e.consume();
    }

    @FXML
    private void onZoomIn() {
        zoomMais();
    }

    @FXML
    private void onZoomOut() {
        zoomMenos();
    }

    private void zoomMais() {
        zoom = Math.min(ZOOM_MAX, zoom + ZOOM_PASSO);
        aplicarZoom();
    }

    private void zoomMenos() {
        zoom = Math.max(ZOOM_MIN, zoom - ZOOM_PASSO);
        aplicarZoom();
    }

    private void aplicarZoom() {
        imgPagina.setFitWidth(LARGURA_BASE * zoom);
        lblZoom.setText(Math.round(zoom * 100) + "%");
    }

    private void alternarTelaCheia() {
        if (raiz.getScene() != null && raiz.getScene().getWindow() instanceof Stage s) {
            s.setFullScreen(!s.isFullScreen());
        }
    }

    private void atualizarInfo() {
        lblInfo.setText("Pagina " + (indice + 1) + " de " + paginas.size());
    }

    @FXML
    private void onVoltar() {
        if (fichaRoot != null && raiz.getScene() != null) {
            raiz.getScene().setRoot(fichaRoot);
        }
    }
}

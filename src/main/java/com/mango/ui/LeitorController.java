package com.mango.ui;

import com.mango.config.Config;
import com.mango.model.Capitulo;
import com.mango.model.Manga;
import com.mango.model.Pagina;
import com.mango.service.LeitorService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Leitor de capítulos (UC2) — modo página única, com a página centralizada.
 *
 * <p>Cobre: navegação por teclado/mouse/botões, zoom 25%–400% (RN2.6, FA3),
 * pré-carga de 2 páginas (RN2.1), persistência de progresso a cada página
 * (RN2.2) com conclusão na última (RN2.3), retomada (FA5), salto direto
 * (FA4) e placeholder com "Tentar novamente" em falha de página (EX1).</p>
 */
public class LeitorController {

    private static final Logger log = LoggerFactory.getLogger(LeitorController.class);

    private static final double ZOOM_PASSO = 0.25;
    private static final double LARGURA_BASE = 800;

    @FXML private BorderPane raiz;
    @FXML private Label lblTitulo;
    @FXML private Label lblPagina;
    @FXML private Label lblZoom;
    @FXML private Label lblAviso;
    @FXML private TextField txtIrPara;
    @FXML private StackPane painel;
    @FXML private ImageView imgPagina;
    @FXML private VBox painelErro;
    @FXML private Label lblErro;
    @FXML private ProgressIndicator progresso;
    @FXML private Button btnAnterior;
    @FXML private Button btnProxima;

    private final LeitorService service = new LeitorService();
    private final Map<Integer, Image> cache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        final Thread t = new Thread(r, "leitor-precarga");
        t.setDaemon(true);
        return t;
    });

    private Manga manga;
    private Capitulo capitulo;
    private List<Pagina> paginas = List.of();
    private int indice;
    private double zoom = 1.0;

    @FXML
    public void initialize() {
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, this::aoTeclar);
        raiz.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {                       // FA3: Ctrl + scroll
                ajustarZoom(e.getDeltaY() > 0 ? ZOOM_PASSO : -ZOOM_PASSO);
                e.consume();
            }
        });
    }

    /** Ponto de entrada: carrega as páginas e abre na posição salva (FA5). */
    public void abrir(final Manga manga, final Capitulo capitulo) {
        this.manga = manga;
        this.capitulo = capitulo;
        lblTitulo.setText(manga.titulo() + " — Cap. "
                + (capitulo.numero().isBlank() ? "—" : capitulo.numero()));

        final Task<List<Pagina>> task = new Task<>() {
            @Override
            protected List<Pagina> call() {
                return service.paginas(capitulo.id());
            }
        };
        task.setOnSucceeded(e -> {
            paginas = task.getValue();
            progresso.setVisible(false);
            irParaPagina(paginaInicial());
            Platform.runLater(raiz::requestFocus);
        });
        task.setOnFailed(e -> {                            // EX2 e falhas de API
            progresso.setVisible(false);
            mostrarErro(task.getException().getMessage());
            log.error("Falha ao carregar capítulo", task.getException());
        });
        progresso.setVisible(true);
        final Thread t = new Thread(task, "leitor-capitulo");
        t.setDaemon(true);
        t.start();
    }

    /** FA5 — retoma na última página vista, se o capítulo não foi concluído. */
    private int paginaInicial() {
        return service.progressoSalvo(manga.id(), capitulo.id())
                .filter(p -> !p.concluido() && p.paginaAtual() > 0
                        && p.paginaAtual() < paginas.size())
                .map(p -> {
                    avisar("Retomando da página " + (p.paginaAtual() + 1));
                    return p.paginaAtual();
                })
                .orElse(0);
    }

    // ------------------------------------------------------------------
    // Navegação
    // ------------------------------------------------------------------

    @FXML
    public void anterior() {
        irParaPagina(indice - 1);
    }

    @FXML
    public void proxima() {
        irParaPagina(indice + 1);
    }

    @FXML
    public void irPara() {                                 // FA4
        try {
            final int n = Integer.parseInt(txtIrPara.getText().strip());
            if (n < 1 || n > paginas.size()) {
                avisar("Informe uma página entre 1 e " + paginas.size());
                return;
            }
            irParaPagina(n - 1);
        } catch (final NumberFormatException ex) {
            avisar("Número de página inválido");
        }
        raiz.requestFocus();
    }

    @FXML
    public void voltar() {
        executor.shutdownNow();
        Navegador.voltar();
    }

    private void aoTeclar(final KeyEvent e) {
        if (txtIrPara.isFocused()) {
            return;
        }
        switch (e.getCode()) {
            case RIGHT, SPACE, PAGE_DOWN -> proxima();
            case LEFT, PAGE_UP -> anterior();
            case HOME -> irParaPagina(0);
            case END -> irParaPagina(paginas.size() - 1);
            case ADD, EQUALS, PLUS -> ajustarZoom(ZOOM_PASSO);
            case SUBTRACT, MINUS -> ajustarZoom(-ZOOM_PASSO);
            case ESCAPE -> voltar();
            default -> {
                return;
            }
        }
        e.consume();
    }

    private void irParaPagina(final int novoIndice) {
        if (paginas.isEmpty() || novoIndice < 0 || novoIndice >= paginas.size()) {
            return;
        }
        indice = novoIndice;
        exibirPagina();
        precarregar();                                     // RN2.1
        salvarProgresso();                                 // RN2.2 / RN2.3
    }

    // ------------------------------------------------------------------
    // Exibição, pré-carga e erro de página
    // ------------------------------------------------------------------

    private void exibirPagina() {
        final Image imagem = cache.computeIfAbsent(indice, this::baixar);
        mostrarImagem(imagem);
        lblPagina.setText("Página " + (indice + 1) + " / " + paginas.size());
        btnAnterior.setDisable(indice == 0);
        btnProxima.setDisable(indice == paginas.size() - 1);
    }

    private void mostrarImagem(final Image imagem) {
        painelErro.setVisible(false);
        imgPagina.setVisible(true);
        imgPagina.setImage(imagem);
        aplicarZoom();

        if (imagem.isError()) {
            falhaDePagina();
        } else if (imagem.getProgress() < 1.0) {
            final int aguardando = indice;
            imagem.errorProperty().addListener((obs, antes, agora) -> {
                if (agora && aguardando == indice) {
                    falhaDePagina();                       // EX1
                }
            });
        }
    }

    /** EX1 — placeholder com "Tentar novamente", sem travar as demais páginas. */
    private void falhaDePagina() {
        imgPagina.setVisible(false);
        lblErro.setText("Não foi possível carregar a página " + (indice + 1) + ".");
        painelErro.setVisible(true);
    }

    @FXML
    public void tentarNovamente() {                        // EX1
        cache.remove(indice);
        exibirPagina();
    }

    /** RN2.1 — mantém as próximas 2 páginas pré-carregadas em background. */
    private void precarregar() {
        for (int i = 1; i <= Config.PRE_CARGA; i++) {
            final int alvo = indice + i;
            if (alvo < paginas.size() && !cache.containsKey(alvo)) {
                executor.submit(() -> cache.computeIfAbsent(alvo, this::baixar));
            }
        }
    }

    private Image baixar(final int i) {
        return new Image(paginas.get(i).url(), true);      // backgroundLoading
    }

    // ------------------------------------------------------------------
    // Zoom (RN2.6)
    // ------------------------------------------------------------------

    @FXML
    public void zoomMais() {
        ajustarZoom(ZOOM_PASSO);
    }

    @FXML
    public void zoomMenos() {
        ajustarZoom(-ZOOM_PASSO);
    }

    private void ajustarZoom(final double delta) {
        zoom = Math.clamp(zoom + delta, Config.ZOOM_MIN, Config.ZOOM_MAX);
        aplicarZoom();
    }

    private void aplicarZoom() {
        imgPagina.setPreserveRatio(true);
        imgPagina.setFitWidth(LARGURA_BASE * zoom);
        lblZoom.setText(Math.round(zoom * 100) + "%");
    }

    // ------------------------------------------------------------------
    // Progresso e avisos
    // ------------------------------------------------------------------

    private void salvarProgresso() {
        final int pagina = indice;
        final int total = paginas.size();
        executor.submit(() -> {
            try {
                service.salvarProgresso(manga.id(), capitulo.id(), pagina, total);
            } catch (final RuntimeException ex) {
                log.warn("Falha ao salvar progresso", ex);
            }
        });
    }

    private void avisar(final String texto) {
        lblAviso.setText(texto);
    }

    private void mostrarErro(final String texto) {
        imgPagina.setVisible(false);
        lblErro.setText(texto);
        painelErro.setVisible(true);
    }
}

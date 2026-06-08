package com.mango.controller;

import com.mango.exception.RegraNegocioException;
import com.mango.model.FiltroBusca;
import com.mango.model.Genero;
import com.mango.model.Manga;
import com.mango.model.ResultadoBusca;
import com.mango.service.MangaDexService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller da tela de catalogo (UC1). Converte eventos da UI em chamadas ao
 * {@link MangaDexService}, executadas em background para manter a interface
 * responsiva, e renderiza os resultados em uma grade de cards.
 *
 * <p>Buscas concorrentes sao protegidas por um contador de geracao: respostas de
 * buscas superadas sao descartadas, evitando que um resultado antigo sobrescreva
 * o atual.</p>
 */
public class CatalogoController {

    private static final Logger log = LoggerFactory.getLogger(CatalogoController.class);

    @FXML private TextField campoBusca;
    @FXML private MenuButton menuGeneros;
    @FXML private Button btnBuscar;
    @FXML private FlowPane painelResultados;
    @FXML private ProgressIndicator progresso;
    @FXML private Label lblStatus;
    @FXML private Button btnAnterior;
    @FXML private Button btnProxima;
    @FXML private Label lblPagina;

    private final MangaDexService service = new MangaDexService();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        final Thread t = new Thread(r, "busca-catalogo");
        t.setDaemon(true);
        return t;
    });

    private FiltroBusca filtroAtual = FiltroBusca.porTitulo("");
    private ResultadoBusca ultimoResultado;
    /** Geracao da busca mais recente; respostas com geracao anterior sao ignoradas. */
    private long geracaoBusca;

    @FXML
    public void initialize() {
        progresso.setVisible(false);
        atualizarPaginacao(null);
        campoBusca.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                onBuscar();
            }
        });
        lblStatus.setText("Digite um titulo e pressione Enter, ou busque sem termo para ver os populares.");
        carregarGeneros();
    }

    @FXML
    private void onBuscar() {
        filtroAtual = new FiltroBusca(campoBusca.getText(), generosSelecionados(), "", 0);
        executarBusca(filtroAtual);
    }

    /** Carrega os generos do MangaDex em background e popula o menu (MNG-32). */
    private void carregarGeneros() {
        final Task<List<Genero>> task = new Task<>() {
            @Override
            protected List<Genero> call() {
                return service.listarGeneros();
            }
        };
        task.setOnSucceeded(e -> {
            for (final Genero g : task.getValue()) {
                final CheckMenuItem item = new CheckMenuItem(g.nome());
                item.setUserData(g.id());
                item.selectedProperty().addListener((o, a, b) -> atualizarRotuloGeneros());
                menuGeneros.getItems().add(item);
            }
        });
        task.setOnFailed(e ->
                log.warn("Nao foi possivel carregar os generos: {}",
                        task.getException() != null ? task.getException().getMessage() : "erro"));
        executor.submit(task);
    }

    /** IDs (UUIDs) dos generos marcados no menu. */
    private List<String> generosSelecionados() {
        final List<String> ids = new ArrayList<>();
        for (final MenuItem item : menuGeneros.getItems()) {
            if (item instanceof CheckMenuItem c && c.isSelected()) {
                ids.add((String) c.getUserData());
            }
        }
        return ids;
    }

    private void atualizarRotuloGeneros() {
        final int n = generosSelecionados().size();
        menuGeneros.setText(n == 0 ? "Generos" : "Generos (" + n + ")");
    }

    @FXML
    private void onAnterior() {
        if (ultimoResultado != null && ultimoResultado.temAnterior()) {
            executarBusca(filtroAtual.naPagina(ultimoResultado.pagina() - 1));
        }
    }

    @FXML
    private void onProxima() {
        if (ultimoResultado != null && ultimoResultado.temProxima()) {
            executarBusca(filtroAtual.naPagina(ultimoResultado.pagina() + 1));
        }
    }

    private void executarBusca(final FiltroBusca filtro) {
        filtroAtual = filtro;
        final long geracao = ++geracaoBusca;
        definirCarregando(true);

        final Task<ResultadoBusca> task = new Task<>() {
            @Override
            protected ResultadoBusca call() {
                return service.buscar(filtro);
            }
        };

        task.setOnSucceeded(e -> {
            if (geracao != geracaoBusca) {
                return;
            }
            definirCarregando(false);
            renderizar(task.getValue());
        });
        task.setOnFailed(e -> {
            if (geracao != geracaoBusca) {
                return;
            }
            definirCarregando(false);
            final Throwable causa = task.getException();
            final String msg = (causa instanceof RegraNegocioException)
                    ? causa.getMessage()
                    : (causa != null ? causa.getMessage() : "Falha desconhecida na busca.");
            lblStatus.setText(msg);
            log.warn("Busca falhou: {}", msg);
        });

        executor.submit(task);
    }

    private void renderizar(final ResultadoBusca resultado) {
        ultimoResultado = resultado;
        painelResultados.getChildren().clear();

        if (resultado.vazio()) {
            lblStatus.setText("Nenhum manga encontrado para os filtros informados.");
            atualizarPaginacao(resultado);
            return;
        }

        for (final Manga manga : resultado.mangas()) {
            painelResultados.getChildren().add(criarCard(manga));
        }

        final String origem = resultado.doCache() ? " (cache)" : "";
        lblStatus.setText(resultado.total() + " resultado(s)" + origem
                + " - pagina " + (resultado.pagina() + 1) + " de " + resultado.totalPaginas());
        atualizarPaginacao(resultado);
    }

    private VBox criarCard(final Manga manga) {
        final ImageView capa = new ImageView();
        capa.setFitWidth(160);
        capa.setFitHeight(230);
        capa.setPreserveRatio(true);
        if (manga.capaUrl() != null) {
            final Image img = new Image(manga.capaUrl(), 160, 230, true, true, true);
            img.errorProperty().addListener((obs, antes, erro) -> {
                if (Boolean.TRUE.equals(erro)) {
                    capa.setImage(null);
                    capa.getStyleClass().add("capa-erro");
                }
            });
            capa.setImage(img);
        } else {
            capa.getStyleClass().add("capa-erro");
        }

        final Label titulo = new Label(manga.titulo());
        titulo.getStyleClass().add("card-titulo");
        titulo.setWrapText(true);
        titulo.setMaxWidth(160);

        final Label status = new Label(manga.status());
        status.getStyleClass().add("card-status");

        final VBox card = new VBox(6, capa, titulo, status);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(184);
        card.setOnMouseClicked(e -> abrirFicha(manga));
        return card;
    }

    private void abrirFicha(final Manga manga) {
        try {
            final Parent catalogoRoot = campoBusca.getScene().getRoot();
            final javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/ficha.fxml"));
            final Parent fichaRoot = loader.load();
            final FichaController controller = loader.getController();
            controller.carregar(service, manga, catalogoRoot);
            campoBusca.getScene().setRoot(fichaRoot);
        } catch (final Exception ex) {
            log.error("Falha ao abrir a ficha do manga {}", manga.id(), ex);
            lblStatus.setText("Nao foi possivel abrir a ficha deste manga.");
        }
    }

    private void atualizarPaginacao(final ResultadoBusca resultado) {
        final boolean temAnterior = resultado != null && resultado.temAnterior();
        final boolean temProxima = resultado != null && resultado.temProxima();
        btnAnterior.setDisable(!temAnterior);
        btnProxima.setDisable(!temProxima);
        lblPagina.setText(resultado == null ? ""
                : "Pagina " + (resultado.pagina() + 1) + "/" + resultado.totalPaginas());
    }

    private void definirCarregando(final boolean carregando) {
        Platform.runLater(() -> {
            progresso.setVisible(carregando);
            btnBuscar.setDisable(carregando);
            if (carregando) {
                lblStatus.setText("Buscando...");
            }
        });
    }
}

package com.mango.ui;

import com.mango.model.FiltroBusca;
import com.mango.model.Genero;
import com.mango.model.Manga;
import com.mango.model.ResultadoBusca;
import com.mango.service.CatalogoService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Tela do catálogo (UC1): busca por título, filtro de gênero, grade de
 * cards com capa e paginação. Toda chamada de rede roda fora da thread
 * de UI, com indicador de progresso visível.
 */
public class CatalogoController {

    private static final Logger log = LoggerFactory.getLogger(CatalogoController.class);

    private static final double CAPA_LARGURA = 150;
    private static final double CAPA_ALTURA = 210;

    @FXML private TextField txtBusca;
    @FXML private ComboBox<Genero> cmbGenero;
    @FXML private Button btnBuscar;
    @FXML private Label lblStatus;
    @FXML private Label lblPagina;
    @FXML private TilePane grade;
    @FXML private ProgressIndicator progresso;
    @FXML private Button btnAnterior;
    @FXML private Button btnProxima;

    private final CatalogoService service = new CatalogoService();
    private FiltroBusca filtroAtual = new FiltroBusca("", null, 0);
    private ResultadoBusca ultimo;

    @FXML
    public void initialize() {
        carregarGeneros();
        executarBusca(filtroAtual);          // FA1: abre com os populares
    }

    @FXML
    public void buscar() {
        final Genero genero = cmbGenero.getValue();
        executarBusca(new FiltroBusca(txtBusca.getText(),
                genero != null && genero.id().isEmpty() ? null : genero, 0));
    }

    @FXML
    public void paginaAnterior() {           // FA3
        if (ultimo != null && ultimo.temAnterior()) {
            executarBusca(filtroAtual.comPagina(filtroAtual.pagina() - 1));
        }
    }

    @FXML
    public void proximaPagina() {            // FA3
        if (ultimo != null && ultimo.temProxima()) {
            executarBusca(filtroAtual.comPagina(filtroAtual.pagina() + 1));
        }
    }

    // ------------------------------------------------------------------

    private void executarBusca(final FiltroBusca filtro) {
        final Task<ResultadoBusca> task = new Task<>() {
            @Override
            protected ResultadoBusca call() {
                return service.buscar(filtro);
            }
        };
        task.setOnSucceeded(e -> {
            filtroAtual = filtro;
            ultimo = task.getValue();
            renderizar(ultimo);
            ocupado(false);
        });
        task.setOnFailed(e -> {
            log.error("Busca falhou", task.getException());
            lblStatus.setText(task.getException().getMessage());
            ocupado(false);
        });
        ocupado(true);
        lblStatus.setText("");
        rodar(task, "busca");
    }

    private void renderizar(final ResultadoBusca resultado) {
        grade.getChildren().clear();

        if (resultado.vazio()) {             // EX4
            lblStatus.setText("Nenhum mangá encontrado para os filtros informados.");
        } else if (resultado.doCache()) {    // FA2
            lblStatus.setText("Resultados do cache local (últimos 30 min).");
        }

        for (final Manga manga : resultado.mangas()) {
            grade.getChildren().add(criarCard(manga));
        }

        lblPagina.setText(resultado.totalPaginas() == 0 ? ""
                : "Página " + (resultado.pagina() + 1) + " de " + resultado.totalPaginas());
        btnAnterior.setDisable(!resultado.temAnterior());
        btnProxima.setDisable(!resultado.temProxima());
    }

    private Region criarCard(final Manga manga) {
        final ImageView capa = new ImageView();
        capa.setFitWidth(CAPA_LARGURA);
        capa.setFitHeight(CAPA_ALTURA);
        capa.setPreserveRatio(false);
        if (manga.capaUrl() != null) {
            capa.setImage(new Image(manga.capaUrl(),
                    CAPA_LARGURA, CAPA_ALTURA, false, true, true));
        }
        final StackPane moldura = new StackPane(capa);
        moldura.getStyleClass().add("card-capa");

        final Label titulo = new Label(manga.titulo());
        titulo.getStyleClass().add("card-titulo");
        titulo.setWrapText(true);
        titulo.setMaxWidth(CAPA_LARGURA);

        final Label status = new Label(manga.status()
                + (manga.ano() != null ? " · " + manga.ano() : ""));
        status.getStyleClass().add("card-status");

        final VBox card = new VBox(6, moldura, titulo, status);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setOnMouseClicked(e -> abrirFicha(manga));
        return card;
    }

    private void abrirFicha(final Manga manga) {
        try {
            final FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/fxml/ficha.fxml"));
            final Parent raiz = loader.load();
            loader.<FichaController>getController().exibir(manga);
            Navegador.ir(raiz);
        } catch (final IOException e) {
            log.error("Falha ao abrir a ficha", e);
        }
    }

    @FXML
    public void abrirBiblioteca() {
        try {
            final Parent raiz =
                    FXMLLoader.load(getClass().getResource("/fxml/biblioteca.fxml"));
            Navegador.ir(raiz);
        } catch (final IOException e) {
            log.error("Falha ao abrir a biblioteca", e);
        }
    }

    private void carregarGeneros() {
        final Task<List<Genero>> task = new Task<>() {
            @Override
            protected List<Genero> call() {
                return service.listarGeneros();
            }
        };
        task.setOnSucceeded(e -> {
            cmbGenero.getItems().add(new Genero("", "Todos os gêneros"));
            cmbGenero.getItems().addAll(task.getValue());
            cmbGenero.getSelectionModel().selectFirst();
        });
        task.setOnFailed(e ->
                log.warn("Não foi possível carregar os gêneros", task.getException()));
        rodar(task, "generos");
    }

    private void ocupado(final boolean valor) {
        progresso.setVisible(valor);
        btnBuscar.setDisable(valor);
    }

    private static void rodar(final Task<?> task, final String nome) {
        final Thread t = new Thread(task, "catalogo-" + nome);
        t.setDaemon(true);
        t.start();
    }
}

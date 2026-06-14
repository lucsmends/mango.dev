package com.mango.ui;

import com.mango.model.Colecao;
import com.mango.model.ItemBiblioteca;
import com.mango.model.Manga;
import com.mango.service.BibliotecaService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
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

/** Tela da biblioteca pessoal (UC3): coleções à esquerda, mangás em grade. */
public class BibliotecaController {

    private static final Logger log = LoggerFactory.getLogger(BibliotecaController.class);
    private static final double CAPA_LARGURA = 150;
    private static final double CAPA_ALTURA = 210;

    @FXML private ListView<Colecao> listaColecoes;
    @FXML private TilePane grade;
    @FXML private Label lblVazio;
    @FXML private Button btnRemover;

    private final BibliotecaService service = new BibliotecaService();

    @FXML
    public void initialize() {
        listaColecoes.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(final Colecao item, final boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : item.nome());
            }
        });
        listaColecoes.getSelectionModel().selectedItemProperty().addListener(
                (o, a, b) -> mostrarColecao(b));
        recarregarColecoes(null);
    }

    @FXML
    public void voltar() {
        Navegador.voltar();
    }

    @FXML
    public void novaLista() {
        final TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Nova lista");
        dlg.setHeaderText("Criar uma nova lista de mangás");
        dlg.setContentText("Nome:");
        dlg.showAndWait().ifPresent(nome -> {
            try {
                final Colecao nova = service.criarColecao(nome);
                recarregarColecoes(nova);
            } catch (final RuntimeException ex) {
                new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
            }
        });
    }

    @FXML
    public void removerLista() {
        final Colecao sel = listaColecoes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            return;
        }
        try {
            service.removerColecao(sel);
            recarregarColecoes(null);
        } catch (final RuntimeException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        }
    }

    private void recarregarColecoes(final Colecao selecionar) {
        final Task<List<Colecao>> task = new Task<>() {
            @Override
            protected List<Colecao> call() {
                return service.listarColecoes();
            }
        };
        task.setOnSucceeded(e -> {
            listaColecoes.getItems().setAll(task.getValue());
            if (selecionar != null) {
                listaColecoes.getItems().stream()
                        .filter(c -> c.id() == selecionar.id()).findFirst()
                        .ifPresent(c -> listaColecoes.getSelectionModel().select(c));
            }
            if (listaColecoes.getSelectionModel().getSelectedItem() == null
                    && !listaColecoes.getItems().isEmpty()) {
                listaColecoes.getSelectionModel().selectFirst();
            }
        });
        task.setOnFailed(e -> log.error("Falha ao listar coleções", task.getException()));
        rodar(task, "colecoes");
    }

    private void mostrarColecao(final Colecao colecao) {
        grade.getChildren().clear();
        if (colecao == null) {
            btnRemover.setDisable(true);
            lblVazio.setText("");
            return;
        }
        btnRemover.setDisable(!colecao.removivel());
        final Task<List<ItemBiblioteca>> task = new Task<>() {
            @Override
            protected List<ItemBiblioteca> call() {
                return service.itens(colecao.id());
            }
        };
        task.setOnSucceeded(e -> {
            final List<ItemBiblioteca> itens = task.getValue();
            lblVazio.setText(itens.isEmpty() ? "Nenhum mangá nesta lista ainda." : "");
            for (final ItemBiblioteca it : itens) {
                grade.getChildren().add(criarCard(it));
            }
        });
        task.setOnFailed(e -> log.error("Falha ao carregar coleção", task.getException()));
        rodar(task, "itens");
    }

    private Region criarCard(final ItemBiblioteca item) {
        final ImageView capa = new ImageView();
        capa.setFitWidth(CAPA_LARGURA);
        capa.setFitHeight(CAPA_ALTURA);
        capa.setPreserveRatio(false);
        if (item.capaUrl() != null) {
            capa.setImage(new Image(item.capaUrl(), CAPA_LARGURA, CAPA_ALTURA, false, true, true));
        }
        final StackPane moldura = new StackPane(capa);
        moldura.getStyleClass().add("card-capa");

        final Label titulo = new Label(item.titulo());
        titulo.getStyleClass().add("card-titulo");
        titulo.setWrapText(true);
        titulo.setMaxWidth(CAPA_LARGURA);

        final VBox card = new VBox(6, moldura, titulo);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setOnMouseClicked(e -> abrirFicha(item));
        return card;
    }

    private void abrirFicha(final ItemBiblioteca item) {
        final Manga manga = new Manga(item.mangaId(), item.titulo(), item.capaUrl(),
                "", null, "", "", List.of());
        try {
            final FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ficha.fxml"));
            final Parent raiz = loader.load();
            loader.<FichaController>getController().exibir(manga);
            Navegador.ir(raiz);
        } catch (final IOException e) {
            log.error("Falha ao abrir a ficha", e);
        }
    }

    private static void rodar(final Task<?> task, final String nome) {
        final Thread t = new Thread(task, "biblioteca-" + nome);
        t.setDaemon(true);
        t.start();
    }
}

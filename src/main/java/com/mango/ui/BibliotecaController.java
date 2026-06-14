package com.mango.ui;

import com.mango.model.Capitulo;
import com.mango.model.Colecao;
import com.mango.model.ItemBiblioteca;
import com.mango.model.LeituraRecente;
import com.mango.model.Manga;
import com.mango.service.BibliotecaService;
import com.mango.service.HistoricoService;
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
import java.util.function.Supplier;

/**
 * Biblioteca pessoal (UC3) com barra lateral: Início (continuar lendo),
 * Favoritos, Histórico e as listas do usuário.
 */
public class BibliotecaController {

    private static final Logger log = LoggerFactory.getLogger(BibliotecaController.class);
    private static final double CAPA_LARGURA = 150;
    private static final double CAPA_ALTURA = 210;

    @FXML private ListView<Colecao> listaColecoes;
    @FXML private TilePane grade;
    @FXML private Label lblVazio;
    @FXML private Label lblSecao;
    @FXML private Button btnRemover;

    private final BibliotecaService service = new BibliotecaService();
    private final HistoricoService historico = new HistoricoService();

    @FXML
    public void initialize() {
        listaColecoes.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(final Colecao item, final boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : item.nome());
            }
        });
        listaColecoes.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                mostrarColecao(b);
            }
        });
        recarregarColecoes(null);
        irInicio();
    }

    @FXML
    public void voltar() {
        Navegador.voltar();
    }

    @FXML
    public void irCatalogo() {
        try {
            final Parent raiz =
                    FXMLLoader.load(getClass().getResource("/fxml/catalogo.fxml"));
            Navegador.ir(raiz);
        } catch (final IOException e) {
            log.error("Falha ao abrir o catálogo", e);
        }
    }

    @FXML
    public void irInicio() {
        listaColecoes.getSelectionModel().clearSelection();
        btnRemover.setDisable(true);
        lblSecao.setText("Início · Continuar lendo");
        carregarLeituras(historico::continuarLendo, "Você ainda não começou nenhuma leitura.");
    }

    @FXML
    public void irHistorico() {
        listaColecoes.getSelectionModel().clearSelection();
        btnRemover.setDisable(true);
        lblSecao.setText("Histórico");
        carregarLeituras(historico::historico, "Seu histórico de leitura está vazio.");
    }

    @FXML
    public void irFavoritos() {
        listaColecoes.getItems().stream()
                .filter(c -> "Favoritos".equalsIgnoreCase(c.nome())).findFirst()
                .ifPresent(c -> listaColecoes.getSelectionModel().select(c));
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
            irInicio();
        } catch (final RuntimeException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        }
    }

    // ------------------------------------------------------------------

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
        });
        task.setOnFailed(e -> log.error("Falha ao listar coleções", task.getException()));
        rodar(task, "colecoes");
    }

    private void mostrarColecao(final Colecao colecao) {
        btnRemover.setDisable(!colecao.removivel());
        lblSecao.setText(colecao.nome());
        grade.getChildren().clear();
        lblVazio.setText("");
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
                grade.getChildren().add(criarCardItem(it));
            }
        });
        task.setOnFailed(e -> log.error("Falha ao carregar coleção", task.getException()));
        rodar(task, "itens");
    }

    private void carregarLeituras(final Supplier<List<LeituraRecente>> fonte, final String msgVazio) {
        grade.getChildren().clear();
        lblVazio.setText("");
        final Task<List<LeituraRecente>> task = new Task<>() {
            @Override
            protected List<LeituraRecente> call() {
                return fonte.get();
            }
        };
        task.setOnSucceeded(e -> {
            final List<LeituraRecente> leituras = task.getValue();
            lblVazio.setText(leituras.isEmpty() ? msgVazio : "");
            for (final LeituraRecente r : leituras) {
                grade.getChildren().add(criarCardLeitura(r));
            }
        });
        task.setOnFailed(e -> log.error("Falha ao carregar leituras", task.getException()));
        rodar(task, "leituras");
    }

    // ------------------------------------------------------------------ cards

    private Region criarCardItem(final ItemBiblioteca item) {
        final VBox card = baseCard(item.capaUrl(), item.titulo(), null);
        card.setOnMouseClicked(e -> abrirFicha(item.mangaId(), item.titulo(), item.capaUrl()));
        return card;
    }

    private Region criarCardLeitura(final LeituraRecente r) {
        final VBox card = baseCard(r.capaUrl(), r.tituloExibicao(), r.legenda());
        card.setOnMouseClicked(e -> abrirLeitura(r));
        return card;
    }

    private VBox baseCard(final String capaUrl, final String tituloTexto, final String legenda) {
        final ImageView capa = new ImageView();
        capa.setFitWidth(CAPA_LARGURA);
        capa.setFitHeight(CAPA_ALTURA);
        capa.setPreserveRatio(false);
        if (capaUrl != null) {
            capa.setImage(new Image(capaUrl, CAPA_LARGURA, CAPA_ALTURA, false, true, true));
        }
        final StackPane moldura = new StackPane(capa);
        moldura.getStyleClass().add("card-capa");

        final Label titulo = new Label(tituloTexto);
        titulo.getStyleClass().add("card-titulo");
        titulo.setWrapText(true);
        titulo.setMaxWidth(CAPA_LARGURA);

        final VBox card = new VBox(6, moldura, titulo);
        if (legenda != null) {
            final Label leg = new Label(legenda);
            leg.getStyleClass().add("card-status");
            card.getChildren().add(leg);
        }
        card.getStyleClass().add("card");
        card.setAlignment(Pos.TOP_CENTER);
        return card;
    }

    private void abrirFicha(final String mangaId, final String titulo, final String capaUrl) {
        final Manga manga = new Manga(mangaId, titulo, capaUrl, "", null, "", "", List.of());
        try {
            final FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ficha.fxml"));
            final Parent raiz = loader.load();
            loader.<FichaController>getController().exibir(manga);
            Navegador.ir(raiz);
        } catch (final IOException e) {
            log.error("Falha ao abrir a ficha", e);
        }
    }

    private void abrirLeitura(final LeituraRecente r) {
        final Manga manga = new Manga(r.mangaId(), r.tituloExibicao(), r.capaUrl(),
                "", null, "", "", List.of());
        final Capitulo cap = new Capitulo(r.capituloId(),
                r.capNumero() == null ? "" : r.capNumero(), "", "", r.totalPaginas());
        try {
            final FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leitor.fxml"));
            final Parent raiz = loader.load();
            loader.<LeitorController>getController().abrir(manga, cap);
            Navegador.ir(raiz);
        } catch (final IOException e) {
            log.error("Falha ao abrir o leitor", e);
        }
    }

    private static void rodar(final Task<?> task, final String nome) {
        final Thread t = new Thread(task, "biblioteca-" + nome);
        t.setDaemon(true);
        t.start();
    }
}

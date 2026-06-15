package com.mango.ui;

import com.mango.model.Capitulo;
import com.mango.model.Colecao;
import com.mango.model.FiltroBusca;
import com.mango.model.Genero;
import com.mango.model.ItemBiblioteca;
import com.mango.model.LeituraRecente;
import com.mango.model.Manga;
import com.mango.model.ResultadoBusca;
import com.mango.service.BibliotecaService;
import com.mango.service.CatalogoService;
import com.mango.service.HistoricoService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tela principal: barra de busca fixa no topo, barra lateral (Início,
 * Favoritos, Histórico e listas) e a grade de conteúdo no centro.
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
    @FXML private Button btnAnterior;
    @FXML private Button btnProxima;
    @FXML private ProgressIndicator progresso;

    @FXML private Label lblSecao;
    @FXML private TilePane grade;
    @FXML private Label lblVazio;

    @FXML private ListView<Colecao> listaColecoes;
    @FXML private Button btnRemover;

    private final CatalogoService service = new CatalogoService();
    private final BibliotecaService biblioteca = new BibliotecaService();
    private final HistoricoService historico = new HistoricoService();

    private FiltroBusca filtroAtual = new FiltroBusca("", null, 0);
    private ResultadoBusca ultimo;

    @FXML
    public void initialize() {
        carregarGeneros();
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

    // ----------------------------------------------------------- lateral

    @FXML
    public void irInicio() {
        listaColecoes.getSelectionModel().clearSelection();
        lblSecao.setText("Início");
        executarBusca(new FiltroBusca("", null, 0));      // FA1: populares
    }

    @FXML
    public void irFavoritos() {
        listaColecoes.getItems().stream()
                .filter(c -> "Favoritos".equalsIgnoreCase(c.nome())).findFirst()
                .ifPresent(c -> listaColecoes.getSelectionModel().select(c));
    }

    @FXML
    public void irHistorico() {
        listaColecoes.getSelectionModel().clearSelection();
        lblSecao.setText("Histórico");
        desabilitarPaginacao();
        carregarLeituras(historico::historico, "Seu histórico de leitura está vazio.");
    }

    // ----------------------------------------------------------- busca

    @FXML
    public void buscar() {
        listaColecoes.getSelectionModel().clearSelection();
        lblSecao.setText("Catálogo");
        final Genero genero = cmbGenero.getValue();
        executarBusca(new FiltroBusca(txtBusca.getText(),
                genero != null && genero.id().isEmpty() ? null : genero, 0));
    }

    @FXML
    public void paginaAnterior() {
        if (ultimo != null && ultimo.temAnterior()) {
            executarBusca(filtroAtual.comPagina(filtroAtual.pagina() - 1));
        }
    }

    @FXML
    public void proximaPagina() {
        if (ultimo != null && ultimo.temProxima()) {
            executarBusca(filtroAtual.comPagina(filtroAtual.pagina() + 1));
        }
    }

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
            renderizarBusca(ultimo);
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

    private void renderizarBusca(final ResultadoBusca r) {
        grade.getChildren().clear();
        lblVazio.setText("");
        if (r.vazio()) {
            lblStatus.setText("Nenhum mangá encontrado para os filtros informados.");
        } else if (r.doCache()) {
            lblStatus.setText("Resultados do cache local (últimos 30 min).");
        }
        for (final Manga m : r.mangas()) {
            grade.getChildren().add(criarCardManga(m));
        }
        lblPagina.setText(r.totalPaginas() == 0 ? ""
                : "Página " + (r.pagina() + 1) + " de " + r.totalPaginas());
        btnAnterior.setDisable(!r.temAnterior());
        btnProxima.setDisable(!r.temProxima());
    }

    // ----------------------------------------------------------- coleções

    private void recarregarColecoes(final Colecao selecionar) {
        final Task<List<Colecao>> task = new Task<>() {
            @Override
            protected List<Colecao> call() {
                return biblioteca.listarColecoes();
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
        desabilitarPaginacao();
        grade.getChildren().clear();
        lblVazio.setText("");
        final Task<List<ItemBiblioteca>> task = new Task<>() {
            @Override
            protected List<ItemBiblioteca> call() {
                return biblioteca.itens(colecao.id());
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

    @FXML
    public void novaLista() {
        final TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Nova lista");
        dlg.setHeaderText("Criar uma nova lista de mangás");
        dlg.setContentText("Nome:");
        dlg.showAndWait().ifPresent(nome -> {
            try {
                recarregarColecoes(biblioteca.criarColecao(nome));
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
            biblioteca.removerColecao(sel);
            recarregarColecoes(null);
            irInicio();
        } catch (final RuntimeException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        }
    }

    // ----------------------------------------------------------- cards

    private VBox criarCardManga(final Manga m) {
        final VBox card = baseCard(m.capaUrl(), m.titulo(),
                m.status() + (m.ano() != null ? " · " + m.ano() : ""));
        card.setOnMouseClicked(e -> abrirFicha(m));
        return card;
    }

    private VBox criarCardItem(final ItemBiblioteca it) {
        final VBox card = baseCard(it.capaUrl(), it.titulo(), null);
        card.setOnMouseClicked(e -> abrirFicha(
                new Manga(it.mangaId(), it.titulo(), it.capaUrl(), "", null, "", "", List.of())));
        return card;
    }

    private VBox criarCardLeitura(final LeituraRecente r) {
        final VBox card = baseCard(r.capaUrl(), r.tituloExibicao(), r.legenda());
        card.setOnMouseClicked(e -> abrirLeitura(r));
        return card;
    }

    private VBox baseCard(final String capaUrl, final String tituloTexto, final String legenda) {
        final ImageView capa = new ImageView();
        capa.setFitWidth(CAPA_LARGURA);
        capa.setFitHeight(CAPA_ALTURA);
        capa.setPreserveRatio(false);
        final Rectangle clip = new Rectangle(CAPA_LARGURA, CAPA_ALTURA);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        capa.setClip(clip);
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
        card.setOnMouseEntered(e -> animar(card, 1.04));
        card.setOnMouseExited(e -> animar(card, 1.0));
        return card;
    }

    private void abrirFicha(final Manga manga) {
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

    // ----------------------------------------------------------- util

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
        task.setOnFailed(e -> log.warn("Não foi possível carregar os gêneros", task.getException()));
        rodar(task, "generos");
    }

    private void desabilitarPaginacao() {
        btnAnterior.setDisable(true);
        btnProxima.setDisable(true);
        lblPagina.setText("");
    }

    private void ocupado(final boolean valor) {
        progresso.setVisible(valor);
        btnBuscar.setDisable(valor);
    }

    private static void animar(final Node alvo, final double escala) {
        final ScaleTransition st = new ScaleTransition(Duration.millis(130), alvo);
        st.setToX(escala);
        st.setToY(escala);
        st.play();
    }

    private static void rodar(final Task<?> task, final String nome) {
        final Thread t = new Thread(task, "principal-" + nome);
        t.setDaemon(true);
        t.start();
    }
}

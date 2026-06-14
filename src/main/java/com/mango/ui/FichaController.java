package com.mango.ui;

import com.mango.model.Capitulo;
import com.mango.model.Colecao;
import com.mango.model.Manga;
import com.mango.service.BibliotecaService;
import com.mango.service.CatalogoService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/** Ficha do mangá (UC1) com adição à biblioteca pessoal (UC3). */
public class FichaController {

    private static final Logger log = LoggerFactory.getLogger(FichaController.class);

    @FXML private ImageView imgCapa;
    @FXML private Label lblTitulo;
    @FXML private Label lblMeta;
    @FXML private Label lblGeneros;
    @FXML private Label lblSinopse;
    @FXML private Label lblStatusCapitulos;
    @FXML private ListView<Capitulo> listaCapitulos;
    @FXML private ProgressIndicator progresso;
    @FXML private MenuButton menuBiblioteca;

    private final CatalogoService service = new CatalogoService();
    private final BibliotecaService biblioteca = new BibliotecaService();
    private Manga manga;

    /** Coleções + IDs das que já contêm o mangá (carregadas juntas). */
    private record DadosBiblioteca(List<Colecao> colecoes, Set<Long> doManga) {
    }

    @FXML
    public void initialize() {
        listaCapitulos.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(final Capitulo item, final boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : item.rotulo());
            }
        });
        listaCapitulos.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                abrirLeitor();
            }
        });
    }

    /** Preenche a ficha e dispara o carregamento assíncrono dos capítulos. */
    public void exibir(final Manga manga) {
        this.manga = manga;

        lblTitulo.setText(manga.titulo());
        lblMeta.setText(manga.status()
                + (manga.ano() != null ? " · " + manga.ano() : ""));
        lblGeneros.setText(String.join(" · ", manga.generos()));
        lblSinopse.setText(manga.sinopse() == null || manga.sinopse().isBlank()
                ? "Sem sinopse disponível." : manga.sinopse());
        if (manga.capaUrl() != null) {
            imgCapa.setImage(new Image(manga.capaUrl(), 240, 336, false, true, true));
        }
        carregarCapitulos();
        popularBiblioteca();
    }

    @FXML
    public void voltar() {
        Navegador.voltar();
    }

    @FXML
    public void abrirLeitor() {
        final Capitulo cap = listaCapitulos.getSelectionModel().getSelectedItem();
        if (cap == null) {
            return;
        }
        try {
            final FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/fxml/leitor.fxml"));
            final Parent raiz = loader.load();
            loader.<LeitorController>getController().abrir(manga, cap);
            Navegador.ir(raiz);
        } catch (final IOException e) {
            log.error("Falha ao abrir o leitor", e);
        }
    }

    // ------------------------------------------------------------- biblioteca

    private void popularBiblioteca() {
        final Task<DadosBiblioteca> task = new Task<>() {
            @Override
            protected DadosBiblioteca call() {
                return new DadosBiblioteca(
                        biblioteca.listarColecoes(),
                        biblioteca.colecoesDoManga(manga.id()));
            }
        };
        task.setOnSucceeded(e -> {
            final DadosBiblioteca d = task.getValue();
            menuBiblioteca.getItems().clear();
            for (final Colecao c : d.colecoes()) {
                final CheckMenuItem item = new CheckMenuItem(c.nome());
                item.setSelected(d.doManga().contains(c.id()));
                item.setOnAction(ev -> alternarColecao(c, item.isSelected()));
                menuBiblioteca.getItems().add(item);
            }
            menuBiblioteca.getItems().add(new SeparatorMenuItem());
            final MenuItem nova = new MenuItem("Nova lista…");
            nova.setOnAction(ev -> criarLista());
            menuBiblioteca.getItems().add(nova);
        });
        task.setOnFailed(e -> log.error("Falha ao carregar a biblioteca", task.getException()));
        final Thread t = new Thread(task, "ficha-biblioteca");
        t.setDaemon(true);
        t.start();
    }

    private void alternarColecao(final Colecao colecao, final boolean marcado) {
        try {
            if (marcado) {
                biblioteca.adicionar(colecao.id(), manga);
            } else {
                biblioteca.remover(colecao.id(), manga.id());
            }
        } catch (final RuntimeException ex) {
            log.error("Falha ao atualizar a biblioteca", ex);
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        }
    }

    private void criarLista() {
        final TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Nova lista");
        dlg.setHeaderText("Criar uma nova lista e adicionar este mangá");
        dlg.setContentText("Nome:");
        dlg.showAndWait().ifPresent(nome -> {
            try {
                final Colecao nova = biblioteca.criarColecao(nome);
                biblioteca.adicionar(nova.id(), manga);
                popularBiblioteca();
            } catch (final RuntimeException ex) {
                new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
            }
        });
    }

    // ------------------------------------------------------------- capítulos

    private void carregarCapitulos() {
        final Task<List<Capitulo>> task = new Task<>() {
            @Override
            protected List<Capitulo> call() {
                return service.listarCapitulos(manga.id());
            }
        };
        task.setOnSucceeded(e -> {
            progresso.setVisible(false);
            final List<Capitulo> caps = task.getValue();
            listaCapitulos.getItems().setAll(caps);
            lblStatusCapitulos.setText(caps.isEmpty()
                    ? "Nenhum capítulo disponível em pt-br ou en."
                    : caps.size() + " capítulos · duplo clique para ler");
        });
        task.setOnFailed(e -> {
            progresso.setVisible(false);
            lblStatusCapitulos.setText(task.getException().getMessage());
            log.error("Falha ao listar capítulos", task.getException());
        });
        final Thread t = new Thread(task, "ficha-capitulos");
        t.setDaemon(true);
        t.start();
    }
}

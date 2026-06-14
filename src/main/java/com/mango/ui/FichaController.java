package com.mango.ui;

import com.mango.model.Capitulo;
import com.mango.model.Manga;
import com.mango.service.CatalogoService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/** Ficha do mangá (UC1): capa, metadados, sinopse e lista de capítulos. */
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

    private final CatalogoService service = new CatalogoService();
    private Manga manga;

    @FXML
    public void initialize() {
        listaCapitulos.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
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

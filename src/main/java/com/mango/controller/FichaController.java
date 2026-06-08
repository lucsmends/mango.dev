package com.mango.controller;

import com.mango.model.Capitulo;
import com.mango.model.Manga;
import com.mango.service.LeituraService;
import com.mango.service.MangaDexService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Controller da ficha do manga (UC1). Exibe metadados e a lista de capitulos;
 * o duplo-clique em um capitulo abre o leitor (UC2).
 */
public class FichaController {

    private static final Logger log = LoggerFactory.getLogger(FichaController.class);

    @FXML private ImageView imgCapa;
    @FXML private Label lblTitulo;
    @FXML private Label lblAutor;
    @FXML private Label lblStatus;
    @FXML private Label lblGeneros;
    @FXML private Label lblSinopse;
    @FXML private ListView<Capitulo> listaCapitulos;
    @FXML private Label lblStatusCapitulos;

    private MangaDexService service;
    private Manga manga;
    private Parent catalogoRoot;

    public void carregar(final MangaDexService service, final Manga manga, final Parent catalogoRoot) {
        this.service = service;
        this.manga = manga;
        this.catalogoRoot = catalogoRoot;

        lblTitulo.setText(manga.titulo());
        lblAutor.setText(manga.autor() == null ? "Autor desconhecido" : manga.autor());
        lblStatus.setText("Status: " + manga.status());
        lblGeneros.setText(manga.generos().isEmpty() ? "" : String.join(" - ", manga.generos()));
        lblSinopse.setText(manga.sinopse());
        if (manga.capaUrl() != null) {
            imgCapa.setImage(new Image(manga.capaUrl(), 240, 340, true, true, true));
        }

        configurarLista();
        carregarCapitulos(manga.id());
    }

    private void configurarLista() {
        listaCapitulos.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(final Capitulo item, final boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : item.rotulo() + "   [" + item.idioma() + "]");
            }
        });
        listaCapitulos.setOnMouseClicked(e -> {
            final Capitulo sel = listaCapitulos.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && sel != null) {
                abrirLeitor(sel);
            }
        });
    }

    private void abrirLeitor(final Capitulo capitulo) {
        try {
            final Parent fichaRoot = imgCapa.getScene().getRoot();
            final FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leitor.fxml"));
            final Parent leitorRoot = loader.load();
            final LeitorController controller = loader.getController();
            controller.carregar(new LeituraService(), manga, capitulo, fichaRoot);
            imgCapa.getScene().setRoot(leitorRoot);
        } catch (final Exception ex) {
            log.error("Falha ao abrir o leitor do capitulo {}", capitulo.id(), ex);
            lblStatusCapitulos.setText("Nao foi possivel abrir o leitor.");
        }
    }

    private void carregarCapitulos(final String mangaId) {
        lblStatusCapitulos.setText("Carregando capitulos...");
        final Task<List<Capitulo>> task = new Task<>() {
            @Override
            protected List<Capitulo> call() {
                return service.listarCapitulos(mangaId);
            }
        };
        task.setOnSucceeded(e -> {
            final var capitulos = task.getValue();
            listaCapitulos.getItems().setAll(capitulos);
            lblStatusCapitulos.setText(capitulos.isEmpty()
                    ? "Este manga ainda nao possui capitulos no idioma preferido."
                    : capitulos.size() + " capitulo(s). Clique duas vezes para ler.");
        });
        task.setOnFailed(e -> {
            final Throwable causa = task.getException();
            lblStatusCapitulos.setText(causa != null ? causa.getMessage() : "Falha ao carregar capitulos.");
            log.warn("Falha ao listar capitulos de {}", mangaId, causa);
        });
        final Thread t = new Thread(task, "lista-capitulos");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onVoltar() {
        if (catalogoRoot != null) {
            imgCapa.getScene().setRoot(catalogoRoot);
        }
    }
}

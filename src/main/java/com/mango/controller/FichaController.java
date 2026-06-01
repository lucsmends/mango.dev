package com.mango.controller;

import com.mango.model.Capitulo;
import com.mango.model.Manga;
import com.mango.service.MangaDexService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller da ficha do mangá (UC1). Exibe os metadados e carrega a lista de
 * capítulos em background. A leitura de um capítulo pertence ao UC2 (em
 * desenvolvimento por outro membro), então aqui apenas sinalizamos o gancho.
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
    private Parent catalogoRoot;

    /** Recebe o serviço, o mangá selecionado e a raiz do catálogo para o "voltar". */
    public void carregar(final MangaDexService service, final Manga manga, final Parent catalogoRoot) {
        this.service = service;
        this.catalogoRoot = catalogoRoot;

        lblTitulo.setText(manga.titulo());
        lblAutor.setText(manga.autor() == null ? "Autor desconhecido" : manga.autor());
        lblStatus.setText("Status: " + manga.status());
        lblGeneros.setText(manga.generos().isEmpty() ? "" : String.join(" · ", manga.generos()));
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
                setText(vazio || item == null ? null
                        : item.rotulo() + "   [" + item.idioma() + "]");
            }
        });
        // Gancho para o UC2: ao concluir o leitor, substituir este aviso pela navegação.
        listaCapitulos.setOnMouseClicked(e -> {
            final Capitulo sel = listaCapitulos.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && sel != null) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Leitura de capítulos (UC2) em desenvolvimento.\nSelecionado: " + sel.rotulo())
                        .showAndWait();
            }
        });
    }

    private void carregarCapitulos(final String mangaId) {
        lblStatusCapitulos.setText("Carregando capítulos...");
        final Task<java.util.List<Capitulo>> task = new Task<>() {
            @Override
            protected java.util.List<Capitulo> call() {
                return service.listarCapitulos(mangaId);
            }
        };
        task.setOnSucceeded(e -> {
            final var capitulos = task.getValue();
            listaCapitulos.getItems().setAll(capitulos);
            lblStatusCapitulos.setText(capitulos.isEmpty()
                    ? "Este mangá ainda não possui capítulos no idioma preferido."   // EX2-like
                    : capitulos.size() + " capítulo(s) disponível(is).");
        });
        task.setOnFailed(e -> {
            final Throwable causa = task.getException();
            lblStatusCapitulos.setText(causa != null ? causa.getMessage() : "Falha ao carregar capítulos.");
            log.warn("Falha ao listar capítulos de {}", mangaId, causa);
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

package com.mango.ui;

import com.mango.model.Relatorio;
import com.mango.model.Relatorio.Contagem;
import com.mango.service.RelatorioService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Tela de relatórios de leitura (UC3): estatísticas e exportação CSV. */
public class RelatorioController {

    private static final Logger log = LoggerFactory.getLogger(RelatorioController.class);

    private static final String TUDO = "Todo o período";
    private static final String MES = "Último mês";
    private static final String ANO = "Último ano";

    @FXML private ComboBox<String> cmbPeriodo;
    @FXML private VBox conteudo;
    @FXML private ProgressIndicator progresso;
    @FXML private Button btnExportar;

    private final RelatorioService service = new RelatorioService();
    private Relatorio atual;

    @FXML
    public void initialize() {
        cmbPeriodo.getItems().addAll(TUDO, MES, ANO);
        cmbPeriodo.getSelectionModel().select(TUDO);
        cmbPeriodo.setOnAction(e -> gerar());
        gerar();
    }

    @FXML
    public void voltar() {
        Navegador.voltar();
    }

    private Instant inicioDoPeriodo() {
        return switch (cmbPeriodo.getValue()) {
            case MES -> Instant.now().minus(30, ChronoUnit.DAYS);
            case ANO -> Instant.now().minus(365, ChronoUnit.DAYS);
            default -> null;                       // todo o período
        };
    }

    private void gerar() {
        final Instant de = inicioDoPeriodo();
        final Task<Relatorio> task = new Task<>() {
            @Override
            protected Relatorio call() {
                return service.gerar(de, null);
            }
        };
        task.setOnSucceeded(e -> {
            progresso.setVisible(false);
            atual = task.getValue();
            renderizar(atual);
        });
        task.setOnFailed(e -> {
            progresso.setVisible(false);
            log.error("Falha ao gerar relatório", task.getException());
            new Alert(Alert.AlertType.WARNING, task.getException().getMessage()).showAndWait();
        });
        progresso.setVisible(true);
        btnExportar.setDisable(true);
        final Thread t = new Thread(task, "relatorio");
        t.setDaemon(true);
        t.start();
    }

    private void renderizar(final Relatorio r) {
        conteudo.getChildren().clear();
        btnExportar.setDisable(false);

        final FlowPane metricas = new FlowPane(16, 16);
        metricas.getChildren().addAll(
                metrica(String.valueOf(r.mangasLidos()), "Mangás lidos"),
                metrica(String.valueOf(r.capitulosConcluidos()), "Capítulos concluídos"),
                metrica(String.valueOf(r.capitulosEmAndamento()), "Em andamento"),
                metrica(String.valueOf(r.paginasLidas()), "Páginas lidas"),
                metrica(r.tempoFormatado(), "Tempo de leitura"));
        conteudo.getChildren().addAll(secao("Resumo"), metricas);

        conteudo.getChildren().addAll(secao("Mangás mais lidos"),
                ranking(r.topMangas(), " págs", "Você ainda não leu nenhuma página."));
        conteudo.getChildren().addAll(secao("Gêneros favoritos"),
                ranking(r.topGeneros(), "", "Adicione mangás à biblioteca para ver seus gêneros favoritos."));
    }

    private Label secao(final String texto) {
        final Label l = new Label(texto);
        l.getStyleClass().add("titulo-ficha");
        return l;
    }

    private VBox metrica(final String valor, final String legenda) {
        final Label num = new Label(valor);
        num.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #FF9F1C;");
        final Label leg = new Label(legenda);
        leg.getStyleClass().add("card-status");
        final VBox box = new VBox(4, num, leg);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("card-capa");
        box.setMinWidth(150);
        box.setStyle(box.getStyle() + "-fx-padding: 16;");
        return box;
    }

    private VBox ranking(final List<Contagem> itens, final String sufixo, final String vazio) {
        final VBox box = new VBox(6);
        if (itens.isEmpty()) {
            final Label l = new Label(vazio);
            l.getStyleClass().add("status");
            box.getChildren().add(l);
            return box;
        }
        int pos = 1;
        for (final Contagem c : itens) {
            final Label l = new Label(pos++ + ". " + c.rotulo() + "  —  " + c.valor() + sufixo);
            l.getStyleClass().add("card-titulo");
            box.getChildren().add(l);
        }
        return box;
    }

    @FXML
    public void exportar() {
        if (atual == null) {
            return;
        }
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar relatório");
        chooser.setInitialFileName("relatorio-mango.csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV (UTF-8)", "*.csv"));
        final var arquivo = chooser.showSaveDialog(conteudo.getScene().getWindow());
        if (arquivo == null) {
            return;
        }
        try {
            Files.write(arquivo.toPath(),
                    service.exportarCsv(atual).getBytes(StandardCharsets.UTF_8));
        } catch (final IOException ex) {
            log.error("Falha ao exportar CSV", ex);
            new Alert(Alert.AlertType.WARNING,
                    "Não foi possível salvar o arquivo: " + ex.getMessage()).showAndWait();
        }
    }
}

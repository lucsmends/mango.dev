package com.mango;

import com.mango.repository.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto de entrada da aplicação Mango.
 *
 * <p>Inicializa o banco H2 local e carrega a tela de catálogo (UC1). As regras
 * de negócio ficam na camada Service; este Main apenas orquestra o bootstrap.</p>
 */
public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final String VERSAO = "1.0.0-SNAPSHOT";

    @Override
    public void start(final Stage primaryStage) throws Exception {
        log.info("Iniciando Mango v{}", VERSAO);

        Database.inicializar();

        final FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/catalogo.fxml"));
        final Parent root = loader.load();

        final Scene scene = new Scene(root, 1280, 800);
        final var css = getClass().getResource("/css/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        primaryStage.setTitle("Mango — Leitor de Mangás");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        log.info("Janela principal exibida");
    }

    @Override
    public void stop() {
        log.info("Encerrando Mango");
    }

    public static void main(final String[] args) {
        launch(args);
    }
}

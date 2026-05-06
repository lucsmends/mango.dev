package com.mango;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto de entrada da aplicação Mango.
 *
 * <p>Inicializa o JavaFX, carrega a tela inicial (catálogo) e configura
 * o banco H2 local. As regras de negócio ficam na camada Service; este
 * Main apenas orquestra o bootstrap.</p>
 */
public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(final Stage primaryStage) throws Exception {
        log.info("Iniciando Mango v{}", getClass().getPackage().getImplementationVersion());

        // TODO Sprint 2: carregar FXML real da tela de catálogo
        // FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/catalogo.fxml"));
        // Scene scene = new Scene(loader.load(), 1280, 800);

        primaryStage.setTitle("Mango — Leitor de Mangás");
        // primaryStage.setScene(scene);
        primaryStage.setWidth(1280);
        primaryStage.setHeight(800);
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

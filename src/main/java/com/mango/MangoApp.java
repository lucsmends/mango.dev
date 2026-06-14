package com.mango;

import com.mango.db.Database;
import com.mango.ui.Navegador;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bootstrap da aplicação: inicializa o H2 e abre o catálogo (UC1). */
public class MangoApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MangoApp.class);

    @Override
    public void start(final Stage stage) throws Exception {
        Database.inicializar();

        final Parent catalogo = FXMLLoader.load(
                getClass().getResource("/fxml/biblioteca.fxml"));

        Navegador.iniciar(stage, catalogo);
        stage.setTitle("Mango — Leitor de Mangás");
        stage.setWidth(1100);
        stage.setHeight(760);
        stage.show();
        log.info("Mango iniciado.");
    }

    public static void main(final String[] args) {
        launch(args);
    }
}

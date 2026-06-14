package com.mango.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Navegação entre telas: uma única Scene cujo root é trocado, com pilha
 * para "voltar" (catálogo → ficha → leitor).
 */
public final class Navegador {

    private static Scene scene;
    private static final Deque<Parent> PILHA = new ArrayDeque<>();

    private Navegador() {
    }

    public static void iniciar(final Stage stage, final Parent raiz) {
        scene = new Scene(raiz);
        scene.getStylesheets().add(
                Navegador.class.getResource("/css/mango.css").toExternalForm());
        stage.setScene(scene);
    }

    /** Avança para uma nova tela, empilhando a atual. */
    public static void ir(final Parent novaRaiz) {
        PILHA.push(scene.getRoot());
        scene.setRoot(novaRaiz);
        novaRaiz.requestFocus();
    }

    /** Retorna à tela anterior, se houver. */
    public static void voltar() {
        if (!PILHA.isEmpty()) {
            final Parent anterior = PILHA.pop();
            scene.setRoot(anterior);
            anterior.requestFocus();
        }
    }
}

package com.mango;

/**
 * Ponto de entrada do fat-jar: classe sem herança de Application para
 * contornar a checagem de módulos do JavaFX no empacotamento shade.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(final String[] args) {
        MangoApp.main(args);
    }
}

package com.mango;

/**
 * Wrapper de inicialização que NÃO estende {@link javafx.application.Application}.
 *
 * <p>Necessário para empacotar a aplicação como fat-jar via maven-shade-plugin:
 * quando a classe principal estende {@code Application}, a JVM exige os módulos
 * JavaFX no module-path e falha com "JavaFX runtime components are missing".
 * Usando este Launcher como mainClass do shade, o fat-jar roda com
 * {@code java -jar mango.jar} normalmente.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(final String[] args) {
        Main.main(args);
    }
}

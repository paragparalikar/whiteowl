package com.whiteowl.workbench.surface;

/**
 * Entry point for the standalone surface analyzer. Delegating through a class
 * that does not extend {@link javafx.application.Application} lets JavaFX run
 * from the classpath without a module-path configuration.
 */
public final class SurfaceAnalyzerMain {

    private SurfaceAnalyzerMain() {
    }

    public static void main(String[] args) {
        SurfaceAnalyzerApp.main(args);
    }

}

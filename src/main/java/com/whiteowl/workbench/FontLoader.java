package com.whiteowl.workbench;

import javafx.scene.text.Font;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;

@Slf4j
public final class FontLoader {

    private static final double LOAD_SIZE = 12.0;
    private static final String FONT_BASE_PATH = "/fonts/";
    private static final List<String> FONT_FILES = List.of(
            "Montserrat-Thin.ttf",
            "Montserrat-ExtraLight.ttf",
            "Montserrat-Light.ttf",
            "Montserrat-Regular.ttf",
            "Montserrat-Italic.ttf",
            "Montserrat-Medium.ttf",
            "Montserrat-SemiBold.ttf",
            "Montserrat-Bold.ttf"
    );

    private FontLoader() {
    }

    public static void loadAll() {
        int loaded = 0;
        for (String file : FONT_FILES) {
            if (loadFont(file)) {
                loaded++;
            }
        }
        log.info("Loaded {}/{} Montserrat font weights", loaded, FONT_FILES.size());
    }

    private static boolean loadFont(String fileName) {
        InputStream stream = FontLoader.class.getResourceAsStream(FONT_BASE_PATH + fileName);
        if (stream == null) {
            log.warn("Font file not found: {}", fileName);
            return false;
        }
        Font font = Font.loadFont(stream, LOAD_SIZE);
        if (font == null) {
            log.warn("Failed to load font: {}", fileName);
            return false;
        }
        return true;
    }

}

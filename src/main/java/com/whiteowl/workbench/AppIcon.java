package com.whiteowl.workbench;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AppIcon {

    private static final int SIZE_16 = 16;
    private static final int SIZE_32 = 32;
    private static final int SIZE_48 = 48;
    private static final double BG_ARC_RATIO = 0.25;
    private static final java.awt.Color BG_TOP = new java.awt.Color(0x2b, 0x2d, 0x30);
    private static final java.awt.Color BG_BOTTOM = new java.awt.Color(0x1e, 0x1f, 0x22);
    private static final java.awt.Color LETTER_COLOR = new java.awt.Color(0x4a, 0x78, 0xc2);
    private static final String LETTER = "W";
    private static final String FONT_FAMILY = "Montserrat Bold";
    private static final String FALLBACK_FONT = "Segoe UI";
    private static final double FONT_RATIO = 0.6;

    public static void apply(Stage stage) {
        stage.getIcons().setAll(toFxImage(SIZE_16), toFxImage(SIZE_32), toFxImage(SIZE_48));
        applyToTaskbar();
    }

    private static Image toFxImage(int size) {
        return SwingFXUtils.toFXImage(generate(size), null);
    }

    private static void applyToTaskbar() {
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(generate(SIZE_48));
                }
            }
        } catch (Exception e) {
            log.debug("Taskbar icon not supported on this platform");
        }
    }

    private static BufferedImage generate(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        drawBackground(g, size);
        drawLetter(g, size);
        g.dispose();
        return image;
    }

    private static void drawBackground(Graphics2D g, int size) {
        int arc = (int) (size * BG_ARC_RATIO);
        GradientPaint gradient = new GradientPaint(0, 0, BG_TOP, 0, size, BG_BOTTOM);
        g.setPaint(gradient);
        g.fillRoundRect(0, 0, size, size, arc, arc);
    }

    private static void drawLetter(Graphics2D g, int size) {
        int fontSize = (int) (size * FONT_RATIO);
        java.awt.Font font = new java.awt.Font(FONT_FAMILY, java.awt.Font.BOLD, fontSize);
        if (!font.getFamily().contains("Montserrat")) {
            font = new java.awt.Font(FALLBACK_FONT, java.awt.Font.BOLD, fontSize);
        }
        g.setFont(font);
        g.setColor(LETTER_COLOR);
        FontMetrics fm = g.getFontMetrics();
        int x = (size - fm.stringWidth(LETTER)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(LETTER, x, y);
    }

}

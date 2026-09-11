package com.whiteowl.workbench.log;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public final class LogViewerPane extends VBox {

    private static final String WHITEOWL_HOME_PROP = "whiteowl.home";
    private static final String USER_HOME_PROP = "user.home";
    private static final String DEFAULT_HOME_SUFFIX = ".whiteowl";
    private static final String LOGS_DIR = "logs";
    private static final String REGULAR_LOG_FILE = "whiteowl.log";
    private static final String ERROR_LOG_FILE = "whiteowl-error.log";
    private static final String LABEL_APPLICATION = "Application Logs";
    private static final String LABEL_ERROR = "Error Logs";
    private static final String LABEL_RELOAD = "Reload";
    private static final String PANE_STYLE = "log-viewer-pane";
    private static final String HEADER_STYLE = "log-viewer-header";
    private static final String TEXT_AREA_STYLE = "log-viewer-text-area";
    private static final String TOGGLE_BUTTON_STYLE = "toggle-btn";
    private static final String TOGGLE_GROUP_STYLE = "toggle-group-container";
    private static final String RELOAD_BUTTON_STYLE = "log-viewer-reload-button";
    private static final String RELOAD_ICON_STYLE = "log-viewer-reload-icon";
    private static final String NO_LOG_FILE = "Log file not found: %s";
    private static final String READ_ERROR = "Failed to read log file: %s";
    private static final int ICON_SIZE = 14;
    private static final int HEADER_SPACING = 8;
    private static final long MAX_READ_BYTES = 512 * 1024;

    private final Path logDir;
    private final TextArea logArea;
    private final ToggleGroup toggleGroup;
    private String regularContent = "";
    private String errorContent = "";
    private boolean showingError;

    public LogViewerPane() {
        this.logDir = resolveLogDir();
        this.logArea = createLogArea();
        this.toggleGroup = new ToggleGroup();
        getStyleClass().add(PANE_STYLE);
        buildLayout();
        loadLogs();
    }

    private void buildLayout() {
        HBox header = buildHeader();
        VBox.setVgrow(logArea, Priority.ALWAYS);
        getChildren().addAll(header, logArea);
    }

    private HBox buildHeader() {
        ToggleButton appToggle = createToggleButton(LABEL_APPLICATION);
        ToggleButton errorToggle = createToggleButton(LABEL_ERROR);
        appToggle.setToggleGroup(toggleGroup);
        errorToggle.setToggleGroup(toggleGroup);
        appToggle.setSelected(true);
        toggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
                return;
            }
            showingError = newToggle == errorToggle;
            refreshDisplay();
        });
        HBox toggleBox = new HBox(appToggle, errorToggle);
        toggleBox.getStyleClass().add(TOGGLE_GROUP_STYLE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button reloadButton = buildReloadButton();
        HBox header = new HBox(HEADER_SPACING, spacer, toggleBox, reloadButton);
        header.setAlignment(Pos.CENTER_RIGHT);
        header.getStyleClass().add(HEADER_STYLE);
        return header;
    }

    private ToggleButton createToggleButton(String text) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add(TOGGLE_BUTTON_STYLE);
        button.setFocusTraversable(false);
        return button;
    }

    private Button buildReloadButton() {
        FontIcon icon = new FontIcon(FluentUiRegularAL.ARROW_SYNC_20);
        icon.setIconSize(ICON_SIZE);
        icon.getStyleClass().add(RELOAD_ICON_STYLE);
        Button button = new Button(LABEL_RELOAD, icon);
        button.getStyleClass().add(RELOAD_BUTTON_STYLE);
        button.setFocusTraversable(false);
        button.setOnAction(e -> loadLogs());
        return button;
    }

    private TextArea createLogArea() {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(false);
        area.getStyleClass().add(TEXT_AREA_STYLE);
        return area;
    }

    private void loadLogs() {
        Thread loader = new Thread(() -> {
            String regular = readLogFile(logDir.resolve(REGULAR_LOG_FILE));
            String error = readLogFile(logDir.resolve(ERROR_LOG_FILE));
            Platform.runLater(() -> {
                regularContent = regular;
                errorContent = error;
                refreshDisplay();
            });
        });
        loader.setDaemon(true);
        loader.setName("log-viewer-loader");
        loader.start();
    }

    private void refreshDisplay() {
        logArea.setText(showingError ? errorContent : regularContent);
        logArea.positionCaret(logArea.getLength());
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    private String readLogFile(Path filePath) {
        File file = filePath.toFile();
        if (!file.exists()) {
            return String.format(NO_LOG_FILE, filePath);
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long fileLength = raf.length();
            long startPos = Math.max(0, fileLength - MAX_READ_BYTES);
            raf.seek(startPos);
            if (startPos > 0) {
                raf.readLine();
            }
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = raf.readLine()) != null) {
                sb.append(new String(line.getBytes("ISO-8859-1"), "UTF-8")).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            log.error(READ_ERROR, filePath, e);
            return String.format(READ_ERROR, e.getMessage());
        }
    }

    private Path resolveLogDir() {
        String home = System.getProperty(WHITEOWL_HOME_PROP,
                System.getProperty(USER_HOME_PROP) + File.separator + DEFAULT_HOME_SUFFIX);
        return Paths.get(home, LOGS_DIR);
    }

}

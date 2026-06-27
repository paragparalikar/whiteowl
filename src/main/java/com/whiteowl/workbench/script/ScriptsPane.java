package com.whiteowl.workbench.script;

import com.whiteowl.core.indicator.script.IndicatorDsl;
import com.whiteowl.core.indicator.script.IndicatorScriptConstants;
import com.whiteowl.core.ranker.script.RankerDsl;
import com.whiteowl.core.ranker.script.RankerScriptConstants;
import com.whiteowl.core.screener.script.ScreenerDsl;
import com.whiteowl.core.screener.script.ScreenerScriptConstants;
import com.whiteowl.core.script.ScriptDescriptor;
import com.whiteowl.core.script.ScriptRepository;
import com.whiteowl.core.script.ScriptType;
import com.whiteowl.workbench.common.ConfirmationDialog;
import groovy.lang.Script;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.whiteowl.core.indicator.script.IndicatorScriptConstants.INDICATORS_DIR;
import static com.whiteowl.core.ranker.script.RankerScriptConstants.RANKERS_DIR;
import static com.whiteowl.core.screener.script.ScreenerScriptConstants.SCREENERS_DIR;
import static com.whiteowl.workbench.script.ScriptEditorConstants.DEFAULT_INDICATOR_NAME;
import static com.whiteowl.workbench.script.ScriptEditorConstants.DEFAULT_RANKER_NAME;
import static com.whiteowl.workbench.script.ScriptEditorConstants.DEFAULT_SCREENER_NAME;
import static com.whiteowl.workbench.script.ScriptEditorConstants.INDICATOR_DSL_REFERENCE;
import static com.whiteowl.workbench.script.ScriptEditorConstants.RANKER_DSL_REFERENCE;
import static com.whiteowl.workbench.script.ScriptEditorConstants.SCREENER_DSL_REFERENCE;

@Slf4j
public final class ScriptsPane extends VBox {

    private static final String PANE_STYLE = "scripts-pane";
    private static final String HEADER_STYLE = "scripts-header";
    private static final String TYPE_COMBO_STYLE = "scripts-type-combo";
    private static final String TOOLBAR_BUTTON_STYLE = "scripts-toolbar-button";
    private static final String TOOLBAR_ICON_STYLE = "scripts-toolbar-icon";
    private static final String LIST_STYLE = "scripts-list";
    private static final String DELETE_CONFIRM_TITLE = "Delete Script";
    private static final String DELETE_CONFIRM_FORMAT = "Delete script '%s'?";
    private static final String DELETE_CONFIRM_BUTTON = "Delete";
    private static final String LABEL_INDICATOR = "Indicators";
    private static final String LABEL_SCREENER = "Screeners";
    private static final String LABEL_RANKER = "Rankers";
    private static final int TOOLBAR_ICON_SIZE = 12;
    private static final int TOOLBAR_GAP = 2;
    private static final int HEADER_GAP = 4;

    private final Map<ScriptType, ScriptRepository> repositories = new EnumMap<>(ScriptType.class);
    private final Map<ScriptType, Class<? extends Script>> baseClasses = new EnumMap<>(ScriptType.class);
    private final Map<ScriptType, String> dslReferences = new EnumMap<>(ScriptType.class);
    private final Map<ScriptType, String> defaultNames = new EnumMap<>(ScriptType.class);
    private final ComboBox<ScriptType> typeCombo;
    private final ListView<ScriptDescriptor> scriptList;
    private BiConsumer<ScriptDescriptor, ScriptEditorPane> onEditScript;

    public ScriptsPane(ScriptRepository indicatorRepo, ScriptRepository screenerRepo,
                       ScriptRepository rankerRepo) {
        repositories.put(ScriptType.INDICATOR, indicatorRepo);
        repositories.put(ScriptType.SCREENER, screenerRepo);
        repositories.put(ScriptType.RANKER, rankerRepo);
        baseClasses.put(ScriptType.INDICATOR, IndicatorDsl.class);
        baseClasses.put(ScriptType.SCREENER, ScreenerDsl.class);
        baseClasses.put(ScriptType.RANKER, RankerDsl.class);
        dslReferences.put(ScriptType.INDICATOR, INDICATOR_DSL_REFERENCE);
        dslReferences.put(ScriptType.SCREENER, SCREENER_DSL_REFERENCE);
        dslReferences.put(ScriptType.RANKER, RANKER_DSL_REFERENCE);
        defaultNames.put(ScriptType.INDICATOR, DEFAULT_INDICATOR_NAME);
        defaultNames.put(ScriptType.SCREENER, DEFAULT_SCREENER_NAME);
        defaultNames.put(ScriptType.RANKER, DEFAULT_RANKER_NAME);
        indicatorRepo.seedSamples(IndicatorScriptConstants.SAMPLES);
        screenerRepo.seedSamples(ScreenerScriptConstants.SAMPLES);
        rankerRepo.seedSamples(RankerScriptConstants.SAMPLES);
        this.typeCombo = buildTypeCombo();
        this.scriptList = buildScriptList();
        getStyleClass().add(PANE_STYLE);
        buildLayout();
        refreshList();
    }

    public void setOnEditScript(BiConsumer<ScriptDescriptor, ScriptEditorPane> handler) {
        this.onEditScript = handler;
    }

    private void buildLayout() {
        Button newBtn = buildToolbarButton(FluentUiRegularAL.ADD_16, e -> createScript());
        Button editBtn = buildToolbarButton(FluentUiRegularAL.EDIT_16, e -> editSelectedScript());
        Button deleteBtn = buildToolbarButton(FluentUiRegularAL.DELETE_16, e -> deleteSelectedScript());
        HBox toolbar = new HBox(TOOLBAR_GAP, newBtn, editBtn, deleteBtn);
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(HEADER_GAP, typeCombo, toolbar);
        header.getStyleClass().add(HEADER_STYLE);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(typeCombo, Priority.ALWAYS);
        VBox.setVgrow(scriptList, Priority.ALWAYS);
        getChildren().addAll(header, scriptList);
    }

    private ComboBox<ScriptType> buildTypeCombo() {
        ComboBox<ScriptType> combo = new ComboBox<>();
        combo.getItems().addAll(ScriptType.INDICATOR, ScriptType.SCREENER, ScriptType.RANKER);
        combo.setValue(ScriptType.INDICATOR);
        combo.getStyleClass().add(TYPE_COMBO_STYLE);
        combo.setCellFactory(lv -> new ScriptTypeCell());
        combo.setButtonCell(new ScriptTypeCell());
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.valueProperty().addListener((obs, oldVal, newVal) -> refreshList());
        return combo;
    }

    private ListView<ScriptDescriptor> buildScriptList() {
        ListView<ScriptDescriptor> list = new ListView<>();
        list.getStyleClass().add(LIST_STYLE);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ScriptDescriptor item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        list.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ScriptDescriptor selected = list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openEditor(selected);
                }
            }
        });
        return list;
    }

    private Button buildToolbarButton(Ikon ikon, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(TOOLBAR_ICON_SIZE);
        icon.getStyleClass().add(TOOLBAR_ICON_STYLE);
        Button button = new Button(null, icon);
        button.getStyleClass().add(TOOLBAR_BUTTON_STYLE);
        button.setOnAction(handler);
        return button;
    }

    private void createScript() {
        ScriptType type = typeCombo.getValue();
        ScriptRepository repository = repositories.get(type);
        String defaultName = defaultNames.get(type);
        try {
            ScriptDescriptor descriptor = repository.create(defaultName);
            scriptList.getItems().add(descriptor);
            scriptList.getSelectionModel().select(descriptor);
            openEditor(descriptor);
        } catch (IOException ex) {
            log.error("Failed to create script: {}", ex.getMessage());
        }
    }

    private void editSelectedScript() {
        ScriptDescriptor selected = scriptList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        openEditor(selected);
    }

    private void deleteSelectedScript() {
        ScriptDescriptor selected = scriptList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ScriptType type = typeCombo.getValue();
        ScriptRepository repository = repositories.get(type);
        ConfirmationDialog dialog = new ConfirmationDialog(
                DELETE_CONFIRM_TITLE,
                String.format(DELETE_CONFIRM_FORMAT, selected.getName()),
                DELETE_CONFIRM_BUTTON);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            try {
                repository.delete(selected);
                scriptList.getItems().remove(selected);
            } catch (IOException ex) {
                log.error("Failed to delete script: {}", ex.getMessage());
            }
        }
    }

    private void openEditor(ScriptDescriptor descriptor) {
        ScriptType type = typeCombo.getValue();
        ScriptRepository repository = repositories.get(type);
        Class<? extends Script> baseClass = baseClasses.get(type);
        String dslRef = dslReferences.get(type);
        String defaultName = defaultNames.get(type);
        ScriptEditorPane editor = new ScriptEditorPane(repository, baseClass, List.of(), defaultName, dslRef);
        if (onEditScript != null) {
            onEditScript.accept(descriptor, editor);
        }
    }

    private void refreshList() {
        ScriptType type = typeCombo.getValue();
        if (type == null) return;
        ScriptRepository repository = repositories.get(type);
        scriptList.getItems().setAll(repository.findAll());
    }

    private String labelFor(ScriptType type) {
        return switch (type) {
            case INDICATOR -> LABEL_INDICATOR;
            case SCREENER -> LABEL_SCREENER;
            case RANKER -> LABEL_RANKER;
            default -> type.name();
        };
    }

    private final class ScriptTypeCell extends ListCell<ScriptType> {

        @Override
        protected void updateItem(ScriptType item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : labelFor(item));
        }

    }

}

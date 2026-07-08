package com.whiteowl.workbench.script;

import com.whiteowl.core.backtest.v2.dsl.TradingStrategyBase;
import com.whiteowl.core.indicator.script.IndicatorDsl;
import com.whiteowl.core.ranker.script.RankerDsl;
import com.whiteowl.core.screener.script.ScreenerDsl;
import com.whiteowl.core.script.ScriptDescriptor;
import com.whiteowl.core.script.ScriptRepository;
import com.whiteowl.core.script.ScriptType;
import com.whiteowl.workbench.common.ConfirmationDialog;
import groovy.lang.Script;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.whiteowl.core.backtest.StrategyScriptConstants.V2_ADDITIONAL_IMPORTS;
import static com.whiteowl.workbench.script.ScriptEditorConstants.DEFAULT_INDICATOR_NAME;
import static com.whiteowl.workbench.script.ScriptEditorConstants.DEFAULT_RANKER_NAME;
import static com.whiteowl.workbench.script.ScriptEditorConstants.DEFAULT_SCREENER_NAME;
import static com.whiteowl.workbench.script.ScriptEditorConstants.DEFAULT_STRATEGY_NAME;
import static com.whiteowl.workbench.script.ScriptEditorConstants.INDICATOR_DSL_REFERENCE;
import static com.whiteowl.workbench.script.ScriptEditorConstants.RANKER_DSL_REFERENCE;
import static com.whiteowl.workbench.script.ScriptEditorConstants.SCREENER_DSL_REFERENCE;
import static com.whiteowl.workbench.script.ScriptEditorConstants.STRATEGY_DSL_REFERENCE;

@Slf4j
public final class ScriptsPane extends VBox {

    private static final String PANE_STYLE = "scripts-pane";
    private static final String TOOLBAR_STYLE = "scripts-toolbar";
    private static final String SEARCH_FIELD_STYLE = "scripts-search-field";
    private static final String ADD_BUTTON_STYLE = "scripts-add-button";
    private static final String TYPE_COMBO_STYLE = "scripts-type-combo";
    private static final String LIST_STYLE = "scripts-list";
    private static final String CELL_NAME_STYLE = "scripts-cell-name";
    private static final String CELL_EDIT_STYLE = "scripts-cell-edit";
    private static final String CELL_DELETE_STYLE = "scripts-cell-delete";
    private static final String ICON_STYLE = "scripts-icon";
    private static final String DELETE_CONFIRM_TITLE = "Delete Script";
    private static final String DELETE_CONFIRM_FORMAT = "Delete script '%s'?";
    private static final String DELETE_CONFIRM_BUTTON = "Delete";
    private static final String SEARCH_PROMPT = "Search scripts...";
    private static final String LABEL_INDICATOR = "Indicators";
    private static final String LABEL_SCREENER = "Screeners";
    private static final String LABEL_RANKER = "Rankers";
    private static final String LABEL_STRATEGY = "Strategies";
    private static final int ICON_SIZE = 14;
    private static final int CELL_ICON_SIZE = 12;
    private static final int FIXED_CELL_HEIGHT = 24;
    private static final int TOOLBAR_GAP = 4;

    private final Map<ScriptType, ScriptRepository> repositories = new EnumMap<>(ScriptType.class);
    private final Map<ScriptType, Class<? extends Script>> baseClasses = new EnumMap<>(ScriptType.class);
    private final Map<ScriptType, List<String>> additionalImports = new EnumMap<>(ScriptType.class);
    private final Map<ScriptType, String> dslReferences = new EnumMap<>(ScriptType.class);
    private final Map<ScriptType, String> defaultNames = new EnumMap<>(ScriptType.class);
    private final ComboBox<ScriptType> typeCombo;
    private final ListView<ScriptDescriptor> scriptList;
    private String searchText = "";
    private BiConsumer<ScriptDescriptor, ScriptEditorPane> onEditScript;
    private Consumer<ScriptType> onScriptChanged;

    public ScriptsPane(ScriptRepository indicatorRepo, ScriptRepository screenerRepo,
                       ScriptRepository rankerRepo, ScriptRepository strategyRepo) {
        repositories.put(ScriptType.INDICATOR, indicatorRepo);
        repositories.put(ScriptType.SCREENER, screenerRepo);
        repositories.put(ScriptType.RANKER, rankerRepo);
        repositories.put(ScriptType.STRATEGY, strategyRepo);
        baseClasses.put(ScriptType.INDICATOR, IndicatorDsl.class);
        baseClasses.put(ScriptType.SCREENER, ScreenerDsl.class);
        baseClasses.put(ScriptType.RANKER, RankerDsl.class);
        baseClasses.put(ScriptType.STRATEGY, TradingStrategyBase.class);
        additionalImports.put(ScriptType.STRATEGY, V2_ADDITIONAL_IMPORTS);
        dslReferences.put(ScriptType.INDICATOR, INDICATOR_DSL_REFERENCE);
        dslReferences.put(ScriptType.SCREENER, SCREENER_DSL_REFERENCE);
        dslReferences.put(ScriptType.RANKER, RANKER_DSL_REFERENCE);
        dslReferences.put(ScriptType.STRATEGY, STRATEGY_DSL_REFERENCE);
        defaultNames.put(ScriptType.INDICATOR, DEFAULT_INDICATOR_NAME);
        defaultNames.put(ScriptType.SCREENER, DEFAULT_SCREENER_NAME);
        defaultNames.put(ScriptType.RANKER, DEFAULT_RANKER_NAME);
        defaultNames.put(ScriptType.STRATEGY, DEFAULT_STRATEGY_NAME);
        this.typeCombo = buildTypeCombo();
        this.scriptList = buildScriptList();
        getStyleClass().add(PANE_STYLE);
        buildLayout();
        refreshList();
    }

    public void setOnEditScript(BiConsumer<ScriptDescriptor, ScriptEditorPane> handler) {
        this.onEditScript = handler;
    }

    public void setOnScriptChanged(Consumer<ScriptType> handler) {
        this.onScriptChanged = handler;
    }

    private void buildLayout() {
        HBox toolbar = buildToolbar();
        VBox.setVgrow(scriptList, Priority.ALWAYS);
        getChildren().addAll(typeCombo, toolbar, scriptList);
    }

    private HBox buildToolbar() {
        TextField searchField = new TextField();
        searchField.setPromptText(SEARCH_PROMPT);
        searchField.getStyleClass().add(SEARCH_FIELD_STYLE);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchText = newVal == null ? "" : newVal.toLowerCase();
            refreshList();
        });
        HBox.setHgrow(searchField, Priority.ALWAYS);
        Button addButton = new Button();
        FontIcon addIcon = new FontIcon(FluentUiRegularAL.ADD_16);
        addIcon.setIconSize(ICON_SIZE);
        addIcon.getStyleClass().add(ICON_STYLE);
        addButton.setGraphic(addIcon);
        addButton.getStyleClass().add(ADD_BUTTON_STYLE);
        addButton.setFocusTraversable(false);
        addButton.setOnAction(e -> createScript());
        HBox bar = new HBox(TOOLBAR_GAP, searchField, addButton);
        bar.getStyleClass().add(TOOLBAR_STYLE);
        bar.setPadding(new Insets(4));
        return bar;
    }

    private ComboBox<ScriptType> buildTypeCombo() {
        ComboBox<ScriptType> combo = new ComboBox<>();
        combo.getItems().addAll(ScriptType.INDICATOR, ScriptType.SCREENER, ScriptType.RANKER, ScriptType.STRATEGY);
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
        list.setFixedCellSize(FIXED_CELL_HEIGHT);
        list.setCellFactory(lv -> new ScriptCell());
        return list;
    }

    private void createScript() {
        ScriptType type = typeCombo.getValue();
        ScriptRepository repository = repositories.get(type);
        String defaultName = defaultNames.get(type);
        try {
            ScriptDescriptor descriptor = repository.create(defaultName);
            refreshList();
            scriptList.getSelectionModel().select(descriptor);
            notifyScriptChanged(type);
            openEditor(descriptor);
        } catch (IOException ex) {
            log.error("Failed to create script: {}", ex.getMessage());
        }
    }

    private void deleteScript(ScriptDescriptor descriptor) {
        ScriptType type = typeCombo.getValue();
        ScriptRepository repository = repositories.get(type);
        ConfirmationDialog dialog = new ConfirmationDialog(
                DELETE_CONFIRM_TITLE,
                String.format(DELETE_CONFIRM_FORMAT, descriptor.getName()),
                DELETE_CONFIRM_BUTTON);
        dialog.show(getScene().getWindow());
        if (dialog.isConfirmed()) {
            try {
                repository.delete(descriptor);
                refreshList();
                notifyScriptChanged(type);
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
        List<String> imports = additionalImports.getOrDefault(type, List.of());
        ScriptEditorPane editor = new ScriptEditorPane(repository, baseClass, imports, defaultName, dslRef);
        if (onEditScript != null) {
            onEditScript.accept(descriptor, editor);
        }
    }

    private void notifyScriptChanged(ScriptType type) {
        if (onScriptChanged != null) onScriptChanged.accept(type);
    }

    public void refreshList() {
        ScriptType type = typeCombo.getValue();
        if (type == null) return;
        ScriptRepository repository = repositories.get(type);
        List<ScriptDescriptor> all = repository.findAll();
        if (searchText.isEmpty()) {
            scriptList.getItems().setAll(all);
        } else {
            scriptList.getItems().setAll(all.stream()
                    .filter(d -> d.getName().toLowerCase().contains(searchText))
                    .toList());
        }
    }

    private String labelFor(ScriptType type) {
        return switch (type) {
            case INDICATOR -> LABEL_INDICATOR;
            case SCREENER -> LABEL_SCREENER;
            case RANKER -> LABEL_RANKER;
            case STRATEGY -> LABEL_STRATEGY;
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

    private final class ScriptCell extends ListCell<ScriptDescriptor> {

        private final Label nameLabel = new Label();
        private final HBox container = new HBox();

        ScriptCell() {
            nameLabel.getStyleClass().add(CELL_NAME_STYLE);
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            container.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(ScriptDescriptor item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            nameLabel.setText(item.getName());
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            FontIcon editIcon = new FontIcon(FluentUiRegularAL.EDIT_16);
            editIcon.setIconSize(CELL_ICON_SIZE);
            editIcon.getStyleClass().add(ICON_STYLE);
            Button editBtn = new Button(null, editIcon);
            editBtn.getStyleClass().add(CELL_EDIT_STYLE);
            editBtn.setFocusTraversable(false);
            editBtn.setOnAction(e -> openEditor(item));
            FontIcon deleteIcon = new FontIcon(FluentUiRegularAL.DELETE_16);
            deleteIcon.setIconSize(CELL_ICON_SIZE);
            deleteIcon.getStyleClass().add(ICON_STYLE);
            Button deleteBtn = new Button(null, deleteIcon);
            deleteBtn.getStyleClass().add(CELL_DELETE_STYLE);
            deleteBtn.setFocusTraversable(false);
            deleteBtn.setOnAction(e -> deleteScript(item));
            container.getChildren().setAll(nameLabel, spacer, editBtn, deleteBtn);
            setGraphic(container);
            setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) openEditor(item);
            });
        }

    }

}

package com.whiteowl.workbench.alert;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.scripting.screener.Screen;
import com.whiteowl.scripting.screener.ScreenRegistry;
import com.whiteowl.workbench.collection.CollectionType;
import com.whiteowl.workbench.collection.NamedScripCollection;
import com.whiteowl.workbench.common.BaseDialog;
import com.whiteowl.workbench.group.model.Group;
import com.whiteowl.workbench.group.repository.GroupRepository;
import com.whiteowl.workbench.watchlist.model.Watchlist;
import com.whiteowl.workbench.watchlist.repository.WatchlistRepository;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.Getter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;

import java.util.List;

public final class AlertDefinitionDialog extends BaseDialog {

    private static final String DIALOG_TITLE = "Alert Definition";
    private static final String NAME_LABEL = "Name *";
    private static final String SOURCE_TYPE_LABEL = "Source Type *";
    private static final String SOURCE_LABEL = "Source *";
    private static final String SCREEN_LABEL = "Screen *";
    private static final String TIMEFRAME_LABEL = "Timeframe *";
    private static final String ENABLED_LABEL = "Enabled";
    private static final String SAVE_BUTTON = "Save";
    private static final int DIALOG_WIDTH = 420;

    private final GroupRepository groupRepository;
    private final WatchlistRepository watchlistRepository;
    private final ScreenRegistry screenRegistry;
    private final AlertDefinition existing;
    private TextField nameField;
    private ComboBox<CollectionType> sourceTypeCombo;
    private ComboBox<NamedScripCollection> sourceCombo;
    private ComboBox<Screen> screenCombo;
    private ComboBox<Timeframe> timeframeCombo;
    private CheckBox enabledCheckBox;

    @Getter private boolean confirmed;
    @Getter private AlertDefinition alertDefinition;

    public AlertDefinitionDialog(GroupRepository groupRepository,
                                 WatchlistRepository watchlistRepository,
                                 ScreenRegistry screenRegistry) {
        this(groupRepository, watchlistRepository, screenRegistry, null);
    }

    public AlertDefinitionDialog(GroupRepository groupRepository,
                                 WatchlistRepository watchlistRepository,
                                 ScreenRegistry screenRegistry,
                                 AlertDefinition existing) {
        this.groupRepository = groupRepository;
        this.watchlistRepository = watchlistRepository;
        this.screenRegistry = screenRegistry;
        this.existing = existing;
    }

    @Override
    protected String getTitle() {
        return DIALOG_TITLE;
    }

    @Override
    protected Ikon getHeaderIcon() {
        return FluentUiRegularAL.ALERT_24;
    }

    @Override
    protected int getDialogWidth() {
        return DIALOG_WIDTH;
    }

    @Override
    protected String getPrimaryButtonText() {
        return SAVE_BUTTON;
    }

    @Override
    protected Ikon getPrimaryButtonIcon() {
        return FluentUiRegularAL.CHECKMARK_16;
    }

    @Override
    protected void buildFormFields(GridPane grid) {
        nameField = createTextField();
        sourceTypeCombo = createComboBox();
        sourceTypeCombo.getItems().addAll(CollectionType.GROUP, CollectionType.WATCHLIST);
        sourceTypeCombo.setCellFactory(lv -> new CollectionTypeCell());
        sourceTypeCombo.setButtonCell(new CollectionTypeCell());
        sourceCombo = createComboBox();
        sourceCombo.setCellFactory(lv -> new SourceCell());
        sourceCombo.setButtonCell(new SourceCell());
        sourceTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> populateSourceCombo(newVal, null));
        screenCombo = createComboBox();
        screenCombo.getItems().addAll(screenRegistry.getScreens());
        screenCombo.setCellFactory(lv -> new ScreenCell());
        screenCombo.setButtonCell(new ScreenCell());
        timeframeCombo = createComboBox();
        timeframeCombo.getItems().addAll(Timeframe.values());
        enabledCheckBox = createCheckBox(true);

        if (existing != null) {
            nameField.setText(existing.getName());
            CollectionType type = existing.getSourceType() != null ? existing.getSourceType() : CollectionType.GROUP;
            sourceTypeCombo.setValue(type);
            populateSourceCombo(type, existing.getSourceId());
            screenCombo.getItems().stream()
                    .filter(s -> s.getId().equals(existing.getScreenId()))
                    .findFirst()
                    .ifPresent(screenCombo::setValue);
            timeframeCombo.setValue(existing.getTimeframe());
            enabledCheckBox.setSelected(existing.isEnabled());
        } else {
            sourceTypeCombo.setValue(CollectionType.GROUP);
            populateSourceCombo(CollectionType.GROUP, null);
            if (!screenCombo.getItems().isEmpty()) screenCombo.setValue(screenCombo.getItems().get(0));
            timeframeCombo.setValue(Timeframe.DAILY);
        }

        int row = 0;
        addFormRow(grid, NAME_LABEL, nameField, row++);
        addFormRow(grid, SOURCE_TYPE_LABEL, sourceTypeCombo, row++);
        addFormRow(grid, SOURCE_LABEL, sourceCombo, row++);
        addFormRow(grid, SCREEN_LABEL, screenCombo, row++);
        addFormRow(grid, TIMEFRAME_LABEL, timeframeCombo, row++);
        addFormRow(grid, ENABLED_LABEL, enabledCheckBox, row);
    }

    private void populateSourceCombo(CollectionType type, String preselectId) {
        sourceCombo.getItems().clear();
        if (type == CollectionType.GROUP) {
            List<Group> groups = groupRepository.loadAll();
            sourceCombo.getItems().addAll(groups);
        } else if (type == CollectionType.WATCHLIST) {
            List<Watchlist> watchlists = watchlistRepository.loadAll();
            sourceCombo.getItems().addAll(watchlists);
        }
        if (preselectId != null) {
            sourceCombo.getItems().stream()
                    .filter(c -> preselectId.equals(getCollectionId(c)))
                    .findFirst()
                    .ifPresent(sourceCombo::setValue);
        } else if (!sourceCombo.getItems().isEmpty()) {
            sourceCombo.setValue(sourceCombo.getItems().get(0));
        }
    }

    private static String getCollectionId(NamedScripCollection collection) {
        if (collection instanceof Group g) return g.getId();
        if (collection instanceof Watchlist w) return w.getId();
        return null;
    }

    @Override
    protected void onPrimaryAction() {
        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) {
            showError("Name is required");
            return;
        }
        if (sourceTypeCombo.getValue() == null) {
            showError("Source type is required");
            return;
        }
        if (sourceCombo.getValue() == null) {
            showError("Source is required");
            return;
        }
        if (screenCombo.getValue() == null) {
            showError("Screen is required");
            return;
        }
        if (timeframeCombo.getValue() == null) {
            showError("Timeframe is required");
            return;
        }
        String sourceId = getCollectionId(sourceCombo.getValue());
        if (existing != null) {
            existing.setName(name.trim());
            existing.setSourceType(sourceTypeCombo.getValue());
            existing.setSourceId(sourceId);
            existing.setScreenId(screenCombo.getValue().getId());
            existing.setTimeframe(timeframeCombo.getValue());
            existing.setEnabled(enabledCheckBox.isSelected());
            alertDefinition = existing;
        } else {
            alertDefinition = new AlertDefinition(
                    name.trim(),
                    sourceTypeCombo.getValue(),
                    sourceId,
                    screenCombo.getValue().getId(),
                    timeframeCombo.getValue());
            alertDefinition.setEnabled(enabledCheckBox.isSelected());
        }
        confirmed = true;
        closeDialog();
    }

    private static final class CollectionTypeCell extends ListCell<CollectionType> {
        @Override
        protected void updateItem(CollectionType type, boolean empty) {
            super.updateItem(type, empty);
            setText(empty || type == null ? null : type.getDisplayLabel());
        }
    }

    private static final class SourceCell extends ListCell<NamedScripCollection> {
        @Override
        protected void updateItem(NamedScripCollection collection, boolean empty) {
            super.updateItem(collection, empty);
            setText(empty || collection == null ? null : collection.getName());
        }
    }

    private static final class ScreenCell extends ListCell<Screen> {
        @Override
        protected void updateItem(Screen screen, boolean empty) {
            super.updateItem(screen, empty);
            setText(empty || screen == null ? null : screen.getName());
        }
    }
}

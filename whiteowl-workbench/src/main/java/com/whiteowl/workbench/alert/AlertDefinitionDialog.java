package com.whiteowl.workbench.alert;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.scripting.screener.Screen;
import com.whiteowl.scripting.screener.ScreenRegistry;
import com.whiteowl.workbench.common.BaseDialog;
import com.whiteowl.workbench.group.model.Group;
import com.whiteowl.workbench.group.repository.GroupRepository;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.Getter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;

public final class AlertDefinitionDialog extends BaseDialog {

    private static final String DIALOG_TITLE = "Alert Definition";
    private static final String NAME_LABEL = "Name *";
    private static final String GROUP_LABEL = "Group *";
    private static final String SCREEN_LABEL = "Screen *";
    private static final String TIMEFRAME_LABEL = "Timeframe *";
    private static final String ENABLED_LABEL = "Enabled";
    private static final String SAVE_BUTTON = "Save";
    private static final int DIALOG_WIDTH = 420;
    private static final int NAME_ROW = 0;
    private static final int GROUP_ROW = 1;
    private static final int SCREEN_ROW = 2;
    private static final int TIMEFRAME_ROW = 3;
    private static final int ENABLED_ROW = 4;

    private final GroupRepository groupRepository;
    private final ScreenRegistry screenRegistry;
    private final AlertDefinition existing;
    private TextField nameField;
    private ComboBox<Group> groupCombo;
    private ComboBox<Screen> screenCombo;
    private ComboBox<Timeframe> timeframeCombo;
    private CheckBox enabledCheckBox;

    @Getter private boolean confirmed;
    @Getter private AlertDefinition alertDefinition;

    public AlertDefinitionDialog(GroupRepository groupRepository, ScreenRegistry screenRegistry) {
        this(groupRepository, screenRegistry, null);
    }

    public AlertDefinitionDialog(GroupRepository groupRepository, ScreenRegistry screenRegistry, AlertDefinition existing) {
        this.groupRepository = groupRepository;
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
        groupCombo = createComboBox();
        groupCombo.getItems().addAll(groupRepository.loadAll());
        groupCombo.setCellFactory(lv -> new GroupCell());
        groupCombo.setButtonCell(new GroupCell());
        screenCombo = createComboBox();
        screenCombo.getItems().addAll(screenRegistry.getScreens());
        screenCombo.setCellFactory(lv -> new ScreenCell());
        screenCombo.setButtonCell(new ScreenCell());
        timeframeCombo = createComboBox();
        timeframeCombo.getItems().addAll(Timeframe.values());
        enabledCheckBox = createCheckBox(true);

        if (existing != null) {
            nameField.setText(existing.getName());
            groupCombo.getItems().stream()
                    .filter(g -> g.getId().equals(existing.getGroupId()))
                    .findFirst()
                    .ifPresent(groupCombo::setValue);
            screenCombo.getItems().stream()
                    .filter(s -> s.getId().equals(existing.getScreenId()))
                    .findFirst()
                    .ifPresent(screenCombo::setValue);
            timeframeCombo.setValue(existing.getTimeframe());
            enabledCheckBox.setSelected(existing.isEnabled());
        } else {
            if (!groupCombo.getItems().isEmpty()) groupCombo.setValue(groupCombo.getItems().get(0));
            if (!screenCombo.getItems().isEmpty()) screenCombo.setValue(screenCombo.getItems().get(0));
            timeframeCombo.setValue(Timeframe.DAILY);
        }

        addFormRow(grid, NAME_LABEL, nameField, NAME_ROW);
        addFormRow(grid, GROUP_LABEL, groupCombo, GROUP_ROW);
        addFormRow(grid, SCREEN_LABEL, screenCombo, SCREEN_ROW);
        addFormRow(grid, TIMEFRAME_LABEL, timeframeCombo, TIMEFRAME_ROW);
        addFormRow(grid, ENABLED_LABEL, enabledCheckBox, ENABLED_ROW);
    }

    @Override
    protected void onPrimaryAction() {
        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) {
            showError("Name is required");
            return;
        }
        if (groupCombo.getValue() == null) {
            showError("Group is required");
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
        if (existing != null) {
            existing.setName(name.trim());
            existing.setGroupId(groupCombo.getValue().getId());
            existing.setScreenId(screenCombo.getValue().getId());
            existing.setTimeframe(timeframeCombo.getValue());
            existing.setEnabled(enabledCheckBox.isSelected());
            alertDefinition = existing;
        } else {
            alertDefinition = new AlertDefinition(
                    name.trim(),
                    groupCombo.getValue().getId(),
                    screenCombo.getValue().getId(),
                    timeframeCombo.getValue());
            alertDefinition.setEnabled(enabledCheckBox.isSelected());
        }
        confirmed = true;
        closeDialog();
    }

    private static final class GroupCell extends ListCell<Group> {
        @Override
        protected void updateItem(Group group, boolean empty) {
            super.updateItem(group, empty);
            setText(empty || group == null ? null : group.getName());
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

package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.workbench.common.BaseDialog;
import com.whiteowl.workbench.common.ScripSearchField;
import com.whiteowl.workbench.group.model.Group;
import com.whiteowl.workbench.group.repository.GroupRepository;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
public final class IndicatorSettingsDialog extends BaseDialog {

    private static final String APPLY_TEXT = "Apply";
    private static final String SETTINGS_SUFFIX = " Settings";
    private static final int DIALOG_WIDTH = 420;

    private final String indicatorName;
    private final List<IndicatorSetting> settingDefinitions;
    private final Map<String, Object> currentSettings;
    private final BiConsumer<String, Object> settingUpdater;
    private final Runnable onApply;
    private final ScripRepository scripRepository;
    private final GroupRepository groupRepository;
    private final Map<String, Node> controlMap = new LinkedHashMap<>();

    public IndicatorSettingsDialog(ActiveIndicator activeIndicator, ScripRepository scripRepository, Runnable onApply) {
        this(activeIndicator.getIndicator().getName(),
                activeIndicator.getIndicator().getSettings(),
                activeIndicator.getSettings(),
                activeIndicator::updateSetting,
                scripRepository, null,
                onApply);
    }

    public IndicatorSettingsDialog(ActiveSubChartIndicator activeIndicator,
                                   ScripRepository scripRepository, GroupRepository groupRepository,
                                   Runnable onApply) {
        this(activeIndicator.getIndicator().getName(),
                activeIndicator.getIndicator().getSettings(),
                activeIndicator.getSettings(),
                activeIndicator::updateSetting,
                scripRepository, groupRepository,
                onApply);
    }

    public IndicatorSettingsDialog(String name, List<IndicatorSetting> settings,
                                   Map<String, Object> currentValues, ScripRepository scripRepository, Runnable onApply) {
        this(name, settings, currentValues, (k, v) -> currentValues.put(k, v), scripRepository, null, onApply);
    }

    private IndicatorSettingsDialog(String indicatorName,
                                    List<IndicatorSetting> settingDefinitions,
                                    Map<String, Object> currentSettings,
                                    BiConsumer<String, Object> settingUpdater,
                                    ScripRepository scripRepository,
                                    GroupRepository groupRepository,
                                    Runnable onApply) {
        this.indicatorName = indicatorName;
        this.settingDefinitions = settingDefinitions;
        this.currentSettings = currentSettings;
        this.settingUpdater = settingUpdater;
        this.scripRepository = scripRepository;
        this.groupRepository = groupRepository;
        this.onApply = onApply;
    }

    @Override
    protected String getTitle() {
        return indicatorName + SETTINGS_SUFFIX;
    }

    @Override
    protected Ikon getHeaderIcon() {
        return FluentUiRegularAL.DATA_LINE_24;
    }

    @Override
    protected int getDialogWidth() {
        return DIALOG_WIDTH;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void buildFormFields(GridPane grid) {
        int row = 0;
        for (IndicatorSetting setting : settingDefinitions) {
            Node control = buildSettingControl(setting);
            controlMap.put(setting.getName(), control);
            addFormRow(grid, setting.getName(), control, row++);
        }
    }

    @Override
    protected String getPrimaryButtonText() {
        return APPLY_TEXT;
    }

    @Override
    protected Ikon getPrimaryButtonIcon() {
        return FluentUiRegularMZ.SAVE_20;
    }

    @Override
    protected void onPrimaryAction() {
        for (IndicatorSetting setting : settingDefinitions) {
            Node control = controlMap.get(setting.getName());
            if (control == null) continue;
            try {
                if (control instanceof ScripSearchField searchField) {
                    settingUpdater.accept(setting.getName(), searchField.getSelectedScripId());
                } else if (control instanceof ComboBox<?> combo) {
                    settingUpdater.accept(setting.getName(), combo.getValue());
                } else if (control instanceof TextField field) {
                    applyTextFieldValue(setting, field);
                }
            } catch (NumberFormatException ex) {
                log.warn("Invalid value for {}: {}", setting.getName(), ((TextField) control).getText());
                return;
            }
        }
        onApply.run();
        closeDialog();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Node buildSettingControl(IndicatorSetting setting) {
        Object currentValue = currentSettings.get(setting.getName());
        if (setting.getType() == Scrip.class && scripRepository != null) {
            String scripId = currentValue instanceof String s ? s : "";
            return new ScripSearchField(scripRepository, scripId);
        }
        if (setting.getType() == Group.class && groupRepository != null) {
            ComboBox<String> combo = createComboBox();
            List<String> groupNames = groupRepository.loadAll().stream()
                    .map(Group::getName).toList();
            combo.getItems().addAll(groupNames);
            String current = currentValue instanceof String s ? s : "";
            combo.setValue(current);
            return combo;
        }
        if (setting.getType().isEnum()) {
            ComboBox combo = createComboBox();
            combo.getItems().addAll(setting.getType().getEnumConstants());
            combo.setValue(currentValue);
            return combo;
        }
        return createTextField(String.valueOf(currentValue));
    }

    private void applyTextFieldValue(IndicatorSetting setting, TextField field) {
        if (setting.getType() == Integer.class) {
            settingUpdater.accept(setting.getName(), Integer.parseInt(field.getText()));
        } else if (setting.getType() == Double.class) {
            settingUpdater.accept(setting.getName(), Double.parseDouble(field.getText()));
        } else {
            settingUpdater.accept(setting.getName(), field.getText());
        }
    }

}

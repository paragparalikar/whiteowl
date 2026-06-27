package com.whiteowl.workbench.screener;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.screener.Screen;
import com.whiteowl.core.screener.ScreenSetting;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.ScripType;
import com.whiteowl.workbench.common.BaseDialog;
import com.whiteowl.workbench.common.ExchangeFilterCombo;
import com.whiteowl.workbench.common.ScripTypeFilterCombo;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class ScreenConfigDialog extends BaseDialog {

    private static final String DIALOG_SEPARATOR_STYLE = "dialog-divider";
    private static final String SETTINGS_SUFFIX = " Settings";
    private static final String SCRIP_TYPE_LABEL = "Scrip Type";
    private static final String EXCHANGE_LABEL = "Exchange";
    private static final String TIMEFRAME_LABEL = "Timeframe";
    private static final String OFFSET_LABEL = "Offset";
    private static final String PARAMETERS_LABEL = "Parameters";
    private static final int MIN_OFFSET = 0;
    private static final int MAX_OFFSET = Integer.MAX_VALUE;
    private static final String ALL_SCRIP_TYPES_LABEL = "All";
    private static final String RUN_TEXT = "Run";
    private static final String APPLY_TEXT = "Apply";
    private static final int DIALOG_WIDTH = 420;

    private final Screen screen;
    private final ScripType initialScripType;
    private final Exchange initialExchange;
    private final Timeframe initialTimeframe;
    private final int initialOffset;
    private final boolean showRunConfig;
    private final Map<String, Node> paramControlMap = new LinkedHashMap<>();
    private ScripTypeFilterCombo scripTypeCombo;
    private ExchangeFilterCombo exchangeCombo;
    private ComboBox<Timeframe> timeframeCombo;
    private Spinner<Integer> offsetSpinner;
    @Getter private boolean confirmed;

    public ScreenConfigDialog(Screen screen, ScripType scripType, Exchange exchange, Timeframe timeframe, int offset) {
        this.screen = screen;
        this.initialScripType = scripType;
        this.initialExchange = exchange;
        this.initialTimeframe = timeframe;
        this.initialOffset = offset;
        this.showRunConfig = true;
    }

    public ScreenConfigDialog(Screen screen) {
        this.screen = screen;
        this.initialScripType = null;
        this.initialExchange = null;
        this.initialTimeframe = null;
        this.initialOffset = 0;
        this.showRunConfig = false;
    }

    public ScripType getScripType() {
        return scripTypeCombo.getValue();
    }

    public Exchange getExchange() {
        return exchangeCombo.getValue();
    }

    public Timeframe getTimeframe() {
        return timeframeCombo.getValue();
    }

    public int getOffset() {
        return offsetSpinner.getValue();
    }

    @Override
    protected String getTitle() {
        return screen.getName() + SETTINGS_SUFFIX;
    }

    @Override
    protected Ikon getHeaderIcon() {
        return FluentUiRegularAL.FILTER_20;
    }

    @Override
    protected int getDialogWidth() {
        return DIALOG_WIDTH;
    }

    @Override
    protected void buildFormFields(GridPane grid) {
        if (showRunConfig) {
            scripTypeCombo = buildScripTypeCombo();
            exchangeCombo = buildExchangeCombo();
            timeframeCombo = buildTimeframeCombo();
            offsetSpinner = buildOffsetSpinner();
            addFormRow(grid, EXCHANGE_LABEL, exchangeCombo, 0);
            addFormRow(grid, SCRIP_TYPE_LABEL, scripTypeCombo, 1);
            addFormRow(grid, TIMEFRAME_LABEL, timeframeCombo, 2);
            addFormRow(grid, OFFSET_LABEL, offsetSpinner, 3);
        }
    }

    @Override
    protected void addExtraContent(VBox form) {
        List<ScreenSetting> settings = screen.getSettings();
        log.debug("Screen '{}' has {} settings: {}", screen.getName(), settings.size(), settings);
        if (!settings.isEmpty()) {
            if (showRunConfig) {
                Region separator = new Region();
                separator.getStyleClass().add(DIALOG_SEPARATOR_STYLE);
                Label paramsLabel = createLabel(PARAMETERS_LABEL);
                form.getChildren().addAll(separator, paramsLabel);
            }
            GridPane paramsGrid = createFormGrid();
            Map<String, Object> currentValues = screen.getSettingValues();
            int row = 0;
            for (ScreenSetting setting : settings) {
                Node control = createTextField(String.valueOf(currentValues.get(setting.getName())));
                paramControlMap.put(setting.getName(), control);
                addFormRow(paramsGrid, setting.getName(), control, row++);
            }
            form.getChildren().add(paramsGrid);
        }
    }

    @Override
    protected String getPrimaryButtonText() {
        return showRunConfig ? RUN_TEXT : APPLY_TEXT;
    }

    @Override
    protected Ikon getPrimaryButtonIcon() {
        return showRunConfig ? FluentUiRegularMZ.PLAY_20 : FluentUiRegularMZ.SAVE_20;
    }

    @Override
    protected void onPrimaryAction() {
        List<ScreenSetting> settings = screen.getSettings();
        for (ScreenSetting setting : settings) {
            Node control = paramControlMap.get(setting.getName());
            if (control == null) continue;
            try {
                if (control instanceof TextField field) {
                    applyFieldValue(setting, field);
                }
            } catch (NumberFormatException ex) {
                log.warn("Invalid value for {}: {}", setting.getName(), ((TextField) control).getText());
                return;
            }
        }
        confirmed = true;
        closeDialog();
    }

    private void applyFieldValue(ScreenSetting setting, TextField field) {
        if (setting.getType() == Integer.class) {
            screen.updateSetting(setting.getName(), Integer.parseInt(field.getText()));
        } else if (setting.getType() == Double.class) {
            screen.updateSetting(setting.getName(), Double.parseDouble(field.getText()));
        } else {
            screen.updateSetting(setting.getName(), field.getText());
        }
    }

    private Spinner<Integer> buildOffsetSpinner() {
        Spinner<Integer> spinner = createSpinner(MIN_OFFSET, MAX_OFFSET, initialOffset);
        return spinner;
    }

    private ScripTypeFilterCombo buildScripTypeCombo() {
        ScripTypeFilterCombo combo = new ScripTypeFilterCombo(true, initialScripType);
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private ExchangeFilterCombo buildExchangeCombo() {
        ExchangeFilterCombo combo = new ExchangeFilterCombo(true, initialExchange);
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private ComboBox<Timeframe> buildTimeframeCombo() {
        ComboBox<Timeframe> combo = createComboBox();
        combo.getItems().addAll(Timeframe.values());
        combo.setValue(initialTimeframe);
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Timeframe item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayLabel());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Timeframe item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayLabel());
            }
        });
        return combo;
    }

}

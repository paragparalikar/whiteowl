package com.whiteowl.workbench.ranker;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.ranker.Ranker;
import com.whiteowl.core.ranker.RankerSetting;
import com.whiteowl.core.scrip.model.Exchange;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.model.ScripType;
import com.whiteowl.core.scrip.repository.ScripRepository;
import com.whiteowl.workbench.common.BaseDialog;
import com.whiteowl.workbench.common.ScripSearchField;
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
public final class RankerConfigDialog extends BaseDialog {

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
    private static final int DIALOG_WIDTH = 420;

    private final Ranker ranker;
    private final ScripType initialScripType;
    private final Exchange initialExchange;
    private final Timeframe initialTimeframe;
    private final int initialOffset;
    private final ScripRepository scripRepository;
    private final Map<String, Node> paramControlMap = new LinkedHashMap<>();
    private ScripTypeFilterCombo scripTypeCombo;
    private ExchangeFilterCombo exchangeCombo;
    private ComboBox<Timeframe> timeframeCombo;
    private Spinner<Integer> offsetSpinner;
    @Getter private boolean confirmed;

    public RankerConfigDialog(Ranker ranker, ScripType scripType, Exchange exchange,
                               Timeframe timeframe, int offset,
                               ScripRepository scripRepository) {
        this.ranker = ranker;
        this.initialScripType = scripType;
        this.initialExchange = exchange;
        this.initialTimeframe = timeframe;
        this.initialOffset = offset;
        this.scripRepository = scripRepository;
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
        return ranker.getName() + SETTINGS_SUFFIX;
    }

    @Override
    protected Ikon getHeaderIcon() {
        return FluentUiRegularAL.DATA_BAR_HORIZONTAL_24;
    }

    @Override
    protected int getDialogWidth() {
        return DIALOG_WIDTH;
    }

    @Override
    protected void buildFormFields(GridPane grid) {
        scripTypeCombo = buildScripTypeCombo();
        exchangeCombo = buildExchangeCombo();
        timeframeCombo = buildTimeframeCombo();
        offsetSpinner = createSpinner(MIN_OFFSET, MAX_OFFSET, initialOffset);
        addFormRow(grid, EXCHANGE_LABEL, exchangeCombo, 0);
        addFormRow(grid, SCRIP_TYPE_LABEL, scripTypeCombo, 1);
        addFormRow(grid, TIMEFRAME_LABEL, timeframeCombo, 2);
        addFormRow(grid, OFFSET_LABEL, offsetSpinner, 3);
    }

    @Override
    protected void addExtraContent(VBox form) {
        List<RankerSetting> settings = ranker.getSettings();
        if (!settings.isEmpty()) {
            Region separator = new Region();
            separator.getStyleClass().add(DIALOG_SEPARATOR_STYLE);
            Label paramsLabel = createLabel(PARAMETERS_LABEL);
            GridPane paramsGrid = createFormGrid();
            Map<String, Object> currentValues = ranker.getSettingValues();
            int row = 0;
            for (RankerSetting setting : settings) {
                Node control = buildParamControl(setting, currentValues.get(setting.getName()));
                paramControlMap.put(setting.getName(), control);
                addFormRow(paramsGrid, setting.getName(), control, row++);
            }
            form.getChildren().addAll(separator, paramsLabel, paramsGrid);
        }
    }

    @Override
    protected String getPrimaryButtonText() {
        return RUN_TEXT;
    }

    @Override
    protected Ikon getPrimaryButtonIcon() {
        return FluentUiRegularMZ.PLAY_20;
    }

    @Override
    protected void onPrimaryAction() {
        List<RankerSetting> settings = ranker.getSettings();
        for (RankerSetting setting : settings) {
            Node control = paramControlMap.get(setting.getName());
            if (control == null) continue;
            try {
                if (control instanceof ScripSearchField searchField) {
                    ranker.updateSetting(setting.getName(), searchField.getSelectedScripId());
                } else if (control instanceof TextField field) {
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

    private Node buildParamControl(RankerSetting setting, Object currentValue) {
        if (setting.getType() == Scrip.class && scripRepository != null) {
            String scripId = currentValue instanceof String s ? s : "";
            return new ScripSearchField(scripRepository, scripId);
        }
        return createTextField(String.valueOf(currentValue));
    }

    private void applyFieldValue(RankerSetting setting, TextField field) {
        if (setting.getType() == Integer.class) {
            ranker.updateSetting(setting.getName(), Integer.parseInt(field.getText()));
        } else if (setting.getType() == Double.class) {
            ranker.updateSetting(setting.getName(), Double.parseDouble(field.getText()));
        } else {
            ranker.updateSetting(setting.getName(), field.getText());
        }
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

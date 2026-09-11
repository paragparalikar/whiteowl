package com.whiteowl.workbench.charting;

import com.whiteowl.core.trendline.TrendlineSettings;
import com.whiteowl.workbench.common.BaseDialog;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.fluentui.FluentUiRegularMZ;

@Slf4j
public final class TrendlineConfigDialog extends BaseDialog {

    private static final String TITLE_TEXT = "Trendline Settings";
    private static final String APPLY_TEXT = "Apply";
    private static final String PIVOT_LOOKBACK_LABEL = "Pivot Lookback";
    private static final String TOUCH_TOLERANCE_LABEL = "Touch Tolerance %";
    private static final String MIN_TOUCHES_LABEL = "Min Touches";
    private static final String MAX_VIOLATIONS_LABEL = "Max Violations";
    private static final String MIN_SCORE_LABEL = "Min Score";
    private static final String MAX_LINES_LABEL = "Max Lines";
    private static final String SHOW_RESISTANCE_LABEL = "Show Resistance";
    private static final String SHOW_SUPPORT_LABEL = "Show Support";
    private static final int DIALOG_WIDTH = 420;

    private final TrendlineSettings settings;
    private final TextField pivotLookbackField;
    private final TextField touchToleranceField;
    private final TextField minTouchesField;
    private final TextField maxViolationsField;
    private final TextField minScoreField;
    private final TextField maxLinesField;
    private final CheckBox resistanceCheck;
    private final CheckBox supportCheck;
    @Getter private boolean confirmed;

    public TrendlineConfigDialog(TrendlineSettings settings) {
        this.settings = settings;
        this.pivotLookbackField = createTextField(String.valueOf(settings.getPivotLookback()));
        this.touchToleranceField = createTextField(String.valueOf(settings.getTouchTolerancePct()));
        this.minTouchesField = createTextField(String.valueOf(settings.getMinTouches()));
        this.maxViolationsField = createTextField(String.valueOf(settings.getMaxViolations()));
        this.minScoreField = createTextField(String.valueOf(settings.getMinScore()));
        this.maxLinesField = createTextField(String.valueOf(settings.getMaxLines()));
        this.resistanceCheck = createCheckBox(settings.isShowResistance());
        this.supportCheck = createCheckBox(settings.isShowSupport());
    }

    @Override
    protected String getTitle() {
        return TITLE_TEXT;
    }

    @Override
    protected Ikon getHeaderIcon() {
        return FluentUiRegularAL.LINE_HORIZONTAL_1_20;
    }

    @Override
    protected int getDialogWidth() {
        return DIALOG_WIDTH;
    }

    @Override
    protected void buildFormFields(GridPane grid) {
        int row = 0;
        addFormRow(grid, PIVOT_LOOKBACK_LABEL, pivotLookbackField, row++);
        addFormRow(grid, TOUCH_TOLERANCE_LABEL, touchToleranceField, row++);
        addFormRow(grid, MIN_TOUCHES_LABEL, minTouchesField, row++);
        addFormRow(grid, MAX_VIOLATIONS_LABEL, maxViolationsField, row++);
        addFormRow(grid, MIN_SCORE_LABEL, minScoreField, row++);
        addFormRow(grid, MAX_LINES_LABEL, maxLinesField, row++);
        addFormRow(grid, SHOW_RESISTANCE_LABEL, resistanceCheck, row++);
        addFormRow(grid, SHOW_SUPPORT_LABEL, supportCheck, row);
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
        try {
            settings.setPivotLookback(Integer.parseInt(pivotLookbackField.getText()));
            settings.setTouchTolerancePct(Double.parseDouble(touchToleranceField.getText()));
            settings.setMinTouches(Integer.parseInt(minTouchesField.getText()));
            settings.setMaxViolations(Integer.parseInt(maxViolationsField.getText()));
            settings.setMinScore(Double.parseDouble(minScoreField.getText()));
            settings.setMaxLines(Integer.parseInt(maxLinesField.getText()));
            settings.setShowResistance(resistanceCheck.isSelected());
            settings.setShowSupport(supportCheck.isSelected());
            confirmed = true;
            closeDialog();
        } catch (NumberFormatException ex) {
            log.warn("Invalid trendline setting value: {}", ex.getMessage());
        }
    }

}

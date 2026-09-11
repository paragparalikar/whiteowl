package com.whiteowl.workbench.orbbacktest;

import com.whiteowl.core.backtest.rotational.RotationalBacktestReport;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Right-side result pane for the rotational ORB backtest.
 * Manages the lifecycle: progress indicator -> full result display.
 *
 * <p>Result layout (three tabs):
 * <pre>
 * TabPane
 * ├── Tab "Results"  -> ReportPane + MonthlyReturnsPane (side by side)
 * ├── Tab "Charts"   -> SplitPane (VERTICAL, resizable)
 * │   ├── EquityCurvePane  (P&amp;L from zero)
 * │   └── DrawdownPane
 * └── Tab "Trades"   -> TradeTablePane
 * </pre>
 */
public final class RotationalOrbResultPane extends VBox {

    private static final String PANE_STYLE = "rot-orb-result-pane";
    private static final String PROGRESS_SECTION_STYLE = "rot-orb-result-progress-section";
    private static final String PROGRESS_BAR_STYLE = "rot-orb-result-progress-bar";
    private static final String PROGRESS_LABEL_STYLE = "rot-orb-result-progress-label";
    private static final String SPLIT_STYLE = "rot-orb-result-split";
    private static final String ERROR_LABEL_STYLE = "rot-orb-result-error-label";
    private static final String TAB_PANE_STYLE = "rot-orb-result-bottom-tabs";

    private static final String RUNNING_FORMAT = "Loading & running backtest...";
    private static final double PROGRESS_BAR_WIDTH = 400;
    private static final int PROGRESS_SPACING = 12;
    private static final double RESULTS_DIVIDER = 0.35;
    private static final double CHARTS_DIVIDER = 0.65;

    private final ProgressBar progressBar;
    private final Label progressLabel;

    public RotationalOrbResultPane() {
        this.progressBar = buildProgressBar();
        this.progressLabel = buildProgressLabel();
        getStyleClass().add(PANE_STYLE);
        VBox progressSection = buildProgressSection();
        VBox.setVgrow(progressSection, Priority.ALWAYS);
        getChildren().add(progressSection);
    }

    public void updateProgress(String message) {
        progressLabel.setText(message);
    }

    public void displayError(String message) {
        getChildren().clear();
        Label errorLabel = new Label(message);
        errorLabel.getStyleClass().add(ERROR_LABEL_STYLE);
        errorLabel.setWrapText(true);
        VBox errorSection = new VBox(PROGRESS_SPACING, errorLabel);
        errorSection.setAlignment(Pos.CENTER);
        VBox.setVgrow(errorSection, Priority.ALWAYS);
        getChildren().add(errorSection);
    }

    public void displayReport(RotationalBacktestReport report) {
        getChildren().clear();

        // ── Tab 1: Results (metrics + monthly returns side by side) ──
        RotationalOrbReportPane reportPane = new RotationalOrbReportPane();
        reportPane.setReport(report);

        RotationalOrbMonthlyReturnsPane monthlyReturnsPane = new RotationalOrbMonthlyReturnsPane();
        monthlyReturnsPane.setReport(report);

        SplitPane resultsSplit = new SplitPane(reportPane, monthlyReturnsPane);
        resultsSplit.setOrientation(Orientation.HORIZONTAL);
        resultsSplit.setDividerPositions(RESULTS_DIVIDER);
        resultsSplit.getStyleClass().add(SPLIT_STYLE);
        SplitPane.setResizableWithParent(reportPane, false);

        // ── Tab 2: Charts (equity curve / drawdown in vertical split) ──
        RotationalOrbEquityCurvePane equityCurvePane = new RotationalOrbEquityCurvePane();
        equityCurvePane.setReport(report);

        RotationalOrbDrawdownPane drawdownPane = new RotationalOrbDrawdownPane();
        drawdownPane.setReport(report);

        SplitPane chartsSplit = new SplitPane(equityCurvePane, drawdownPane);
        chartsSplit.setOrientation(Orientation.VERTICAL);
        chartsSplit.setDividerPositions(CHARTS_DIVIDER);
        chartsSplit.getStyleClass().add(SPLIT_STYLE);

        // ── Tab 3: Trades ──
        RotationalOrbTradeTablePane tradeTablePane = new RotationalOrbTradeTablePane();
        tradeTablePane.setTrades(report.getTrades());

        // ── Assemble tabs ──
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add(TAB_PANE_STYLE);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("Results", resultsSplit),
                new Tab("Charts", chartsSplit),
                new Tab("Trades", tradeTablePane)
        );

        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().add(tabs);
    }

    // ── Progress section ─────────────────────────────────────────────

    private VBox buildProgressSection() {
        StackPane barWrapper = new StackPane(progressBar);
        barWrapper.setMaxWidth(PROGRESS_BAR_WIDTH);
        VBox section = new VBox(PROGRESS_SPACING, barWrapper, progressLabel);
        section.setAlignment(Pos.CENTER);
        section.getStyleClass().add(PROGRESS_SECTION_STYLE);
        return section;
    }

    private ProgressBar buildProgressBar() {
        ProgressBar bar = new ProgressBar(-1);
        bar.setMaxWidth(PROGRESS_BAR_WIDTH);
        bar.getStyleClass().add(PROGRESS_BAR_STYLE);
        return bar;
    }

    private Label buildProgressLabel() {
        Label label = new Label(RUNNING_FORMAT);
        label.getStyleClass().add(PROGRESS_LABEL_STYLE);
        return label;
    }
}

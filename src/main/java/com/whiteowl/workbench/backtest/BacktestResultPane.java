package com.whiteowl.workbench.backtest;

import com.whiteowl.core.backtest.v2.model.BacktestConfig;
import com.whiteowl.core.backtest.v2.model.BacktestReport;
import com.whiteowl.core.backtest.v2.model.TradeRecord;
import com.whiteowl.core.scrip.repository.ScripRepository;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public final class BacktestResultPane extends VBox {

    private static final String PANE_STYLE = "backtest-result-pane";
    private static final String PROGRESS_SECTION_STYLE = "backtest-result-progress-section";
    private static final String PROGRESS_BAR_STYLE = "backtest-result-progress-bar";
    private static final String PROGRESS_LABEL_STYLE = "backtest-result-progress-label";
    private static final String SPLIT_PANE_STYLE = "backtest-result-split";
    private static final String ERROR_LABEL_STYLE = "backtest-result-error-label";
    private static final String RUNNING_FORMAT = "Testing %d / %d scrips...";
    private static final double PROGRESS_BAR_WIDTH = 400;
    private static final int PROGRESS_SPACING = 12;
    private static final double HORIZONTAL_DIVIDER = 0.35;
    private static final double VERTICAL_DIVIDER = 0.45;

    private final ScripRepository scripRepository;
    private final BacktestConfig config;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final VBox progressSection;
    private Consumer<TradeRecord> onTradeSelected;

    public BacktestResultPane(ScripRepository scripRepository, BacktestConfig config) {
        this.scripRepository = scripRepository;
        this.config = config;
        this.progressBar = buildProgressBar();
        this.progressLabel = buildProgressLabel();
        this.progressSection = buildProgressSection();
        getStyleClass().add(PANE_STYLE);
        VBox.setVgrow(progressSection, Priority.ALWAYS);
        getChildren().add(progressSection);
    }

    public void setOnTradeSelected(Consumer<TradeRecord> handler) {
        this.onTradeSelected = handler;
    }

    public void updateProgress(int completed, int total) {
        double fraction = total > 0 ? (double) completed / total : 0;
        progressBar.setProgress(fraction);
        progressLabel.setText(String.format(RUNNING_FORMAT, completed, total));
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

    public void displayReport(BacktestReport report) {
        getChildren().clear();
        ReportPane reportPane = new ReportPane();
        reportPane.setReport(report, config);
        TradeTablePane tradeTablePane = new TradeTablePane(scripRepository);
        tradeTablePane.setTrades(report.getTrades());
        if (onTradeSelected != null) {
            tradeTablePane.setOnTradeSelected(onTradeSelected);
        }
        EquityCurvePane equityCurvePane = new EquityCurvePane();
        equityCurvePane.setReport(report);
        SplitPane splitPane = buildSplitPane(reportPane, equityCurvePane, tradeTablePane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        getChildren().add(splitPane);
    }

    private SplitPane buildSplitPane(ReportPane reportPane, EquityCurvePane equityCurvePane,
                                      TradeTablePane tradeTablePane) {
        SplitPane rightColumn = new SplitPane(equityCurvePane, tradeTablePane);
        rightColumn.setOrientation(Orientation.VERTICAL);
        rightColumn.setDividerPositions(VERTICAL_DIVIDER);
        SplitPane mainSplit = new SplitPane(reportPane, rightColumn);
        mainSplit.setOrientation(Orientation.HORIZONTAL);
        mainSplit.setDividerPositions(HORIZONTAL_DIVIDER);
        mainSplit.getStyleClass().add(SPLIT_PANE_STYLE);
        SplitPane.setResizableWithParent(reportPane, false);
        return mainSplit;
    }

    private VBox buildProgressSection() {
        StackPane barWrapper = new StackPane(progressBar);
        barWrapper.setMaxWidth(PROGRESS_BAR_WIDTH);
        VBox section = new VBox(PROGRESS_SPACING, barWrapper, progressLabel);
        section.setAlignment(Pos.CENTER);
        section.getStyleClass().add(PROGRESS_SECTION_STYLE);
        return section;
    }

    private ProgressBar buildProgressBar() {
        ProgressBar bar = new ProgressBar(0);
        bar.setMaxWidth(PROGRESS_BAR_WIDTH);
        bar.getStyleClass().add(PROGRESS_BAR_STYLE);
        return bar;
    }

    private Label buildProgressLabel() {
        Label label = new Label();
        label.getStyleClass().add(PROGRESS_LABEL_STYLE);
        return label;
    }

}

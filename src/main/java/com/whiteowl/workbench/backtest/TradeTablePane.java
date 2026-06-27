package com.whiteowl.workbench.backtest;

import com.whiteowl.core.backtest.v2.engine.Side;
import com.whiteowl.core.backtest.v2.model.TradeRecord;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.scrip.repository.ScripRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public final class TradeTablePane extends VBox {

    private static final String TABLE_STYLE = "backtest-trade-table";
    private static final String COL_SCRIP = "Scrip";
    private static final String COL_SIDE = "Side";
    private static final String COL_ENTRY_DATE = "Entry Date";
    private static final String COL_ENTRY_PRICE = "Entry";
    private static final String COL_EXIT_DATE = "Exit Date";
    private static final String COL_EXIT_PRICE = "Exit";
    private static final String COL_PNL = "P&L";
    private static final String COL_PNL_PERCENT = "P&L %";
    private static final String LONG_LABEL = "LONG";
    private static final String SHORT_LABEL = "SHORT";
    private static final Color POSITIVE_COLOR = Color.web("#4ec9b0");
    private static final Color NEGATIVE_COLOR = Color.web("#ef5350");
    private static final int SCRIP_COL_WIDTH = 80;
    private static final int SIDE_COL_WIDTH = 55;
    private static final int DATE_COL_WIDTH = 85;
    private static final int PRICE_COL_WIDTH = 70;
    private static final int PNL_COL_WIDTH = 80;
    private static final int PNL_PCT_COL_WIDTH = 60;
    private static final String EXPORT_BUTTON_STYLE = "backtest-toolbar-button";
    private static final String EXPORT_ICON_STYLE = "backtest-toolbar-icon";
    private static final String TOOLBAR_STYLE = "backtest-trade-toolbar";
    private static final String CSV_DESCRIPTION = "CSV Files";
    private static final String CSV_EXTENSION = "*.csv";
    private static final String DEFAULT_FILE_NAME = "trades.csv";
    private static final String CSV_HEADER = "Scrip,Side,Entry Date,Entry Price,Exit Date,Exit Price,Quantity,Gross P&L,Net P&L,Net P&L %";
    private static final String CSV_ROW_FORMAT = "%s,%s,%s,%.2f,%s,%.2f,%d,%.2f,%.2f,%.2f%%";
    private static final int EXPORT_ICON_SIZE = 12;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yy")
            .withZone(ZoneId.systemDefault());

    private final ScripRepository scripRepository;
    private final TableView<TradeRecord> tableView;
    private final ObservableList<TradeRecord> tradeList;
    private Consumer<TradeRecord> onTradeSelected;

    public TradeTablePane(ScripRepository scripRepository) {
        this.scripRepository = scripRepository;
        this.tradeList = FXCollections.observableArrayList();
        this.tableView = buildTable();
        HBox toolbar = buildToolbar();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        getChildren().addAll(toolbar, tableView);
    }

    public void setOnTradeSelected(Consumer<TradeRecord> handler) {
        this.onTradeSelected = handler;
    }

    public void setTrades(List<TradeRecord> trades) {
        tradeList.setAll(trades);
    }

    private TableView<TradeRecord> buildTable() {
        TableView<TradeRecord> table = new TableView<>(tradeList);
        table.getStyleClass().add(TABLE_STYLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                buildScripColumn(),
                buildSideColumn(),
                buildEntryDateColumn(),
                buildEntryPriceColumn(),
                buildExitDateColumn(),
                buildExitPriceColumn(),
                buildPnlColumn(),
                buildPnlPercentColumn()
        );
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && onTradeSelected != null) {
                onTradeSelected.accept(newVal);
            }
        });
        return table;
    }

    private TableColumn<TradeRecord, String> buildScripColumn() {
        TableColumn<TradeRecord, String> col = new TableColumn<>(COL_SCRIP);
        col.setCellValueFactory(cell -> new SimpleStringProperty(resolveSymbol(cell.getValue().getScripId())));
        col.setPrefWidth(SCRIP_COL_WIDTH);
        return col;
    }

    private TableColumn<TradeRecord, String> buildSideColumn() {
        TableColumn<TradeRecord, String> col = new TableColumn<>(COL_SIDE);
        col.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getSide() == Side.LONG ? LONG_LABEL : SHORT_LABEL));
        col.setPrefWidth(SIDE_COL_WIDTH);
        col.setStyle("-fx-alignment: CENTER;");
        return col;
    }

    private TableColumn<TradeRecord, String> buildEntryDateColumn() {
        TableColumn<TradeRecord, String> col = new TableColumn<>(COL_ENTRY_DATE);
        col.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getEntryTimestamp())));
        col.setPrefWidth(DATE_COL_WIDTH);
        return col;
    }

    private TableColumn<TradeRecord, String> buildEntryPriceColumn() {
        TableColumn<TradeRecord, String> col = new TableColumn<>(COL_ENTRY_PRICE);
        col.setCellValueFactory(cell -> new SimpleStringProperty(formatPrice(cell.getValue().getEntryPrice())));
        col.setPrefWidth(PRICE_COL_WIDTH);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }

    private TableColumn<TradeRecord, String> buildExitDateColumn() {
        TableColumn<TradeRecord, String> col = new TableColumn<>(COL_EXIT_DATE);
        col.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getExitTimestamp())));
        col.setPrefWidth(DATE_COL_WIDTH);
        return col;
    }

    private TableColumn<TradeRecord, String> buildExitPriceColumn() {
        TableColumn<TradeRecord, String> col = new TableColumn<>(COL_EXIT_PRICE);
        col.setCellValueFactory(cell -> new SimpleStringProperty(formatPrice(cell.getValue().getExitPrice())));
        col.setPrefWidth(PRICE_COL_WIDTH);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }

    private TableColumn<TradeRecord, String> buildPnlColumn() {
        TableColumn<TradeRecord, String> col = new TableColumn<>(COL_PNL);
        col.setCellValueFactory(cell -> new SimpleStringProperty(formatPrice(cell.getValue().getNetPnl())));
        col.setPrefWidth(PNL_COL_WIDTH);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        col.setCellFactory(c -> new ColoredCell<>(trade -> trade.getNetPnl()));
        return col;
    }

    private TableColumn<TradeRecord, String> buildPnlPercentColumn() {
        TableColumn<TradeRecord, String> col = new TableColumn<>(COL_PNL_PERCENT);
        col.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.2f%%", cell.getValue().getNetPnlPercent())));
        col.setPrefWidth(PNL_PCT_COL_WIDTH);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        col.setCellFactory(c -> new ColoredCell<>(trade -> trade.getNetPnlPercent()));
        return col;
    }

    private HBox buildToolbar() {
        FontIcon icon = new FontIcon(FluentUiRegularAL.ARROW_DOWNLOAD_16);
        icon.setIconSize(EXPORT_ICON_SIZE);
        icon.getStyleClass().add(EXPORT_ICON_STYLE);
        Button exportButton = new Button(null, icon);
        exportButton.getStyleClass().add(EXPORT_BUTTON_STYLE);
        exportButton.setOnAction(e -> exportToCsv());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(spacer, exportButton);
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        toolbar.getStyleClass().add(TOOLBAR_STYLE);
        return toolbar;
    }

    private void exportToCsv() {
        if (tradeList.isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle(CSV_DESCRIPTION);
        chooser.setInitialFileName(DEFAULT_FILE_NAME);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(CSV_DESCRIPTION, CSV_EXTENSION));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println(CSV_HEADER);
            for (TradeRecord trade : tradeList) {
                writer.println(formatCsvRow(trade));
            }
        } catch (IOException ex) {
            log.error("Failed to export trades: {}", ex.getMessage());
        }
    }

    private String formatCsvRow(TradeRecord trade) {
        return String.format(CSV_ROW_FORMAT,
                resolveSymbol(trade.getScripId()),
                trade.getSide() == Side.LONG ? LONG_LABEL : SHORT_LABEL,
                formatDate(trade.getEntryTimestamp()),
                trade.getEntryPrice(),
                formatDate(trade.getExitTimestamp()),
                trade.getExitPrice(),
                trade.getQuantity(),
                trade.getGrossPnl(),
                trade.getNetPnl(),
                trade.getNetPnlPercent());
    }

    private String resolveSymbol(String scripId) {
        return scripRepository.findById(scripId)
                .map(Scrip::getSymbol)
                .orElse(scripId);
    }

    private static String formatDate(long timestamp) {
        if (timestamp <= 0) return "";
        return DATE_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }

    private static String formatPrice(float value) {
        return String.format("%.2f", value);
    }

    private static final class ColoredCell<S> extends TableCell<TradeRecord, String> {

        private final java.util.function.Function<TradeRecord, Float> valueExtractor;

        ColoredCell(java.util.function.Function<TradeRecord, Float> valueExtractor) {
            this.valueExtractor = valueExtractor;
            setStyle("-fx-alignment: CENTER-RIGHT;");
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                setText(null);
                setTextFill(Color.web("#bbbbbb"));
                return;
            }
            setText(item);
            TradeRecord trade = getTableRow().getItem();
            float val = valueExtractor.apply(trade);
            setTextFill(val >= 0 ? POSITIVE_COLOR : NEGATIVE_COLOR);
        }

    }

}

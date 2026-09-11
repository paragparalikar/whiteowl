package com.whiteowl.workbench.orbbacktest;

import com.whiteowl.core.backtest.rotational.RotationalTrade;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Trade log table for the rotational ORB backtest with CSV export.
 */
@Slf4j
public final class RotationalOrbTradeTablePane extends VBox {

    private static final String TABLE_STYLE = "rot-orb-trade-table";
    private static final String TOOLBAR_STYLE = "rot-orb-trade-toolbar";
    private static final String EXPORT_BUTTON_STYLE = "rot-orb-toolbar-button";
    private static final String EXPORT_ICON_STYLE = "rot-orb-toolbar-icon";

    private static final Color POSITIVE_COLOR = Color.web("#4ec9b0");
    private static final Color NEGATIVE_COLOR = Color.web("#ef5350");

    private static final int DATE_COL_WIDTH = 80;
    private static final int SYMBOL_COL_WIDTH = 100;
    private static final int SIDE_COL_WIDTH = 50;
    private static final int PRICE_COL_WIDTH = 65;
    private static final int PNL_COL_WIDTH = 75;
    private static final int PCT_COL_WIDTH = 60;
    private static final int EXIT_COL_WIDTH = 85;
    private static final int TIME_COL_WIDTH = 55;
    private static final int EXPORT_ICON_SIZE = 12;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yy");

    private static final String CSV_DESCRIPTION = "CSV Files";
    private static final String CSV_EXTENSION = "*.csv";
    private static final String DEFAULT_FILE_NAME = "rotational_orb_trades.csv";
    private static final String CSV_HEADER = "Date,Symbol,Side,Entry Price,Exit Price,Shares,Gross PnL,Net PnL,Net PnL %,Entry Time,OR High,OR Low,Exit Reason";

    private final ObservableList<RotationalTrade> tradeList;
    private final TableView<RotationalTrade> tableView;

    public RotationalOrbTradeTablePane() {
        this.tradeList = FXCollections.observableArrayList();
        this.tableView = buildTable();
        HBox toolbar = buildToolbar();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        getChildren().addAll(toolbar, tableView);
    }

    public void setTrades(List<RotationalTrade> trades) {
        tradeList.setAll(trades);
    }

    // ── Table ────────────────────────────────────────────────────────

    private TableView<RotationalTrade> buildTable() {
        TableView<RotationalTrade> table = new TableView<>(tradeList);
        table.getStyleClass().add(TABLE_STYLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                buildDateColumn(),
                buildSymbolColumn(),
                buildSideColumn(),
                buildEntryPriceColumn(),
                buildExitPriceColumn(),
                buildNetPnlColumn(),
                buildNetPnlPctColumn(),
                buildEntryTimeColumn(),
                buildExitReasonColumn()
        );
        return table;
    }

    private TableColumn<RotationalTrade, String> buildDateColumn() {
        TableColumn<RotationalTrade, String> col = new TableColumn<>("Date");
        col.setCellValueFactory(cell -> new SimpleStringProperty(
                DATE_FMT.format(Instant.ofEpochMilli(cell.getValue().date()).atZone(IST))));
        col.setPrefWidth(DATE_COL_WIDTH);
        return col;
    }

    private TableColumn<RotationalTrade, String> buildSymbolColumn() {
        TableColumn<RotationalTrade, String> col = new TableColumn<>("Symbol");
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().symbol()));
        col.setPrefWidth(SYMBOL_COL_WIDTH);
        return col;
    }

    private TableColumn<RotationalTrade, String> buildSideColumn() {
        TableColumn<RotationalTrade, String> col = new TableColumn<>("Side");
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().side().name()));
        col.setPrefWidth(SIDE_COL_WIDTH);
        col.setStyle("-fx-alignment: CENTER;");
        return col;
    }

    private TableColumn<RotationalTrade, String> buildEntryPriceColumn() {
        TableColumn<RotationalTrade, String> col = new TableColumn<>("Entry");
        col.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.2f", cell.getValue().entryPrice())));
        col.setPrefWidth(PRICE_COL_WIDTH);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }

    private TableColumn<RotationalTrade, String> buildExitPriceColumn() {
        TableColumn<RotationalTrade, String> col = new TableColumn<>("Exit");
        col.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.2f", cell.getValue().exitPrice())));
        col.setPrefWidth(PRICE_COL_WIDTH);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }

    private TableColumn<RotationalTrade, String> buildNetPnlColumn() {
        TableColumn<RotationalTrade, String> col = new TableColumn<>("Net P&L");
        col.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.0f", cell.getValue().netPnl())));
        col.setPrefWidth(PNL_COL_WIDTH);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        col.setCellFactory(c -> new ColoredCell(RotationalTrade::netPnl));
        return col;
    }

    private TableColumn<RotationalTrade, String> buildNetPnlPctColumn() {
        TableColumn<RotationalTrade, String> col = new TableColumn<>("P&L %");
        col.setCellValueFactory(cell -> {
            RotationalTrade t = cell.getValue();
            double pct = t.netReturn() * 100;
            return new SimpleStringProperty(String.format("%.2f%%", pct));
        });
        col.setPrefWidth(PCT_COL_WIDTH);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        col.setCellFactory(c -> new ColoredCell(RotationalTrade::netPnl));
        return col;
    }

    private TableColumn<RotationalTrade, String> buildEntryTimeColumn() {
        TableColumn<RotationalTrade, String> col = new TableColumn<>("Time");
        col.setCellValueFactory(cell -> {
            LocalTime t = cell.getValue().entryTime();
            return new SimpleStringProperty(t != null ? t.toString() : "-");
        });
        col.setPrefWidth(TIME_COL_WIDTH);
        col.setStyle("-fx-alignment: CENTER;");
        return col;
    }

    private TableColumn<RotationalTrade, String> buildExitReasonColumn() {
        TableColumn<RotationalTrade, String> col = new TableColumn<>("Exit Reason");
        col.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().exitReason() != null ? cell.getValue().exitReason().name() : "-"));
        col.setPrefWidth(EXIT_COL_WIDTH);
        return col;
    }

    // ── Toolbar ──────────────────────────────────────────────────────

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
            for (RotationalTrade t : tradeList) {
                writer.println(formatCsvRow(t));
            }
        } catch (IOException ex) {
            log.error("Failed to export trades: {}", ex.getMessage());
        }
    }

    private String formatCsvRow(RotationalTrade t) {
        String date = DATE_FMT.format(Instant.ofEpochMilli(t.date()).atZone(IST));
        return String.format("%s,%s,%s,%.2f,%.2f,%.0f,%.2f,%.2f,%.2f%%,%s,%.2f,%.2f,%s",
                date, t.symbol(), t.side(),
                t.entryPrice(), t.exitPrice(), t.shares(),
                t.grossPnl(), t.netPnl(), t.netReturn() * 100,
                t.entryTime() != null ? t.entryTime() : "-",
                t.orHigh(), t.orLow(),
                t.exitReason() != null ? t.exitReason().name() : "-");
    }

    // ── Colored cell ─────────────────────────────────────────────────

    private static final class ColoredCell extends TableCell<RotationalTrade, String> {

        private final Function<RotationalTrade, Double> valueExtractor;

        ColoredCell(Function<RotationalTrade, Double> valueExtractor) {
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
            RotationalTrade trade = getTableRow().getItem();
            double val = valueExtractor.apply(trade);
            setTextFill(val >= 0 ? POSITIVE_COLOR : NEGATIVE_COLOR);
        }
    }
}

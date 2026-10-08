package com.whiteowl.workbench.surface;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

/**
 * Root content of the surface analyzer window: file loading, filters, axis
 * selection, the 3D surface and the selected-row detail view. Any CSV column
 * can be plotted on any axis; categorical columns are laid out at evenly
 * spaced positions labeled with their distinct values.
 */
@Slf4j
public final class SurfaceAnalyzerPane extends BorderPane {

    private static final String TOOLBAR_STYLE = "sa-toolbar";
    private static final String BUTTON_STYLE = "sa-button";
    private static final String FILE_LABEL_STYLE = "sa-file-label";
    private static final String STATS_LABEL_STYLE = "sa-stats-label";
    private static final String SECTION_LABEL_STYLE = "sa-section-label";
    private static final String CONTROLS_STYLE = "sa-controls";
    private static final String SCROLL_STYLE = "sa-scroll";
    private static final String COMBO_STYLE = "sa-combo";
    private static final String FIELD_STYLE = "sa-field";
    private static final String FILTER_ROW_STYLE = "sa-filter-row";
    private static final String REMOVE_STYLE = "sa-remove-button";
    private static final String LEGEND_STYLE = "sa-legend";
    private static final String DETAIL_HEADER_STYLE = "sa-detail-header";
    private static final String DETAIL_NAME_STYLE = "sa-detail-name";
    private static final String DETAIL_VALUE_STYLE = "sa-detail-value";
    private static final String PLACEHOLDER_STYLE = "sa-placeholder";

    private static final String OPEN_CSV = "Open CSV\u2026";
    private static final String RESET_VIEW = "Reset View";
    private static final String ADD_FILTER = "+ Add Filter";
    private static final String REMOVE = "\u00d7";
    private static final String SECTION_AXES = "Axes";
    private static final String SECTION_FILTERS = "Filters";
    private static final String SECTION_COLUMNS = "Columns";
    private static final String PLACEHOLDER_TEXT = "Open a CSV file to plot";
    private static final String SELECT_COLUMNS_TEXT = "Select at least two columns to plot";
    private static final String NONE_ITEM = "— none —";
    private static final String NO_FILE_TEXT = "No file loaded";
    private static final String AXIS_X = "X";
    private static final String AXIS_Y = "Y";
    private static final String AXIS_Z = "Z";

    private static final double LEFT_PANEL_WIDTH = 280;
    private static final double RIGHT_PANEL_WIDTH = 340;
    private static final double NAME_COLUMN_WIDTH = 160;
    private static final int FIELD_WIDTH = 80;

    private final Label fileLabel = new Label(NO_FILE_TEXT);
    private final Label statsLabel = new Label();
    private final Label placeholder = new Label(PLACEHOLDER_TEXT);
    private final ComboBox<String> xAxisCombo = new ComboBox<>();
    private final ComboBox<String> yAxisCombo = new ComboBox<>();
    private final ComboBox<String> zAxisCombo = new ComboBox<>();
    private final VBox filterBox = new VBox(4);
    private final List<FilterRow> filterRows = new ArrayList<>();
    private final VBox detailsBox = new VBox(6);
    private final Label legendLabel = new Label();
    private final Surface3DPane surfacePane = new Surface3DPane();
    private final Line2DPane linePane = new Line2DPane();
    private final StackPane centerPane = new StackPane(surfacePane, linePane, placeholder);

    private CsvData data;
    private File lastDirectory;

    public SurfaceAnalyzerPane() {
        getStyleClass().add("surface-analyzer-pane");
        setTop(buildToolbar());
        placeholder.getStyleClass().add(PLACEHOLDER_STYLE);
        surfacePane.setOnPointSelected(this::showDetails);
        surfacePane.setVisible(false);
        surfacePane.setMinSize(0, 0);
        linePane.setOnPointSelected(this::showDetails);
        linePane.setVisible(false);
        centerPane.setMinSize(0, 0);
        CollapsiblePanel leftPanel = new CollapsiblePanel(
                buildControlsPanel(), CollapsiblePanel.Side.LEFT, LEFT_PANEL_WIDTH);
        CollapsiblePanel rightPanel = new CollapsiblePanel(
                buildDetailsPanel(), CollapsiblePanel.Side.RIGHT, RIGHT_PANEL_WIDTH);
        SplitPane split = new SplitPane(leftPanel, centerPane, rightPanel);
        split.setOrientation(Orientation.HORIZONTAL);
        SplitPane.setResizableWithParent(leftPanel, false);
        SplitPane.setResizableWithParent(rightPanel, false);
        split.setDividerPositions(0.24, 0.76);
        setCenter(split);
        refreshLegend();
    }

    private HBox buildToolbar() {
        Button openButton = new Button(OPEN_CSV);
        openButton.getStyleClass().add(BUTTON_STYLE);
        openButton.setOnAction(e -> chooseFile());
        Button resetButton = new Button(RESET_VIEW);
        resetButton.getStyleClass().add(BUTTON_STYLE);
        resetButton.setOnAction(e -> surfacePane.resetView());
        fileLabel.getStyleClass().add(FILE_LABEL_STYLE);
        statsLabel.getStyleClass().add(STATS_LABEL_STYLE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, openButton, resetButton, fileLabel, spacer, statsLabel);
        toolbar.getStyleClass().add(TOOLBAR_STYLE);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private ScrollPane buildControlsPanel() {
        VBox controls = new VBox(6);
        controls.getStyleClass().add(CONTROLS_STYLE);
        controls.setPrefWidth(LEFT_PANEL_WIDTH);
        controls.getChildren().addAll(
                sectionLabel(SECTION_AXES),
                axisRow(AXIS_X, xAxisCombo, Surface3DPaneColor.X.color()),
                axisRow(AXIS_Y, yAxisCombo, Surface3DPaneColor.Y.color()),
                axisRow(AXIS_Z, zAxisCombo, Surface3DPaneColor.Z.color()),
                sectionLabel(SECTION_FILTERS),
                filterBox,
                addFilterButton());
        ScrollPane scroll = new ScrollPane(controls);
        scroll.getStyleClass().add(SCROLL_STYLE);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private HBox axisRow(String name, ComboBox<String> combo, Color color) {
        Rectangle swatch = new Rectangle(10, 10, color);
        swatch.setArcWidth(3);
        swatch.setArcHeight(3);
        Label label = new Label(name);
        label.getStyleClass().add(DETAIL_NAME_STYLE);
        label.setMinWidth(12);
        combo.getStyleClass().add(COMBO_STYLE);
        combo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(combo, Priority.ALWAYS);
        combo.valueProperty().addListener((obs, o, n) -> {
            refreshLegend();
            refreshPlot();
        });
        HBox row = new HBox(6, swatch, label, combo);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(SECTION_LABEL_STYLE);
        label.setPadding(new Insets(6, 0, 0, 0));
        return label;
    }

    private Button addFilterButton() {
        Button button = new Button(ADD_FILTER);
        button.getStyleClass().add(BUTTON_STYLE);
        button.setOnAction(e -> addFilterRow());
        return button;
    }

    private void addFilterRow() {
        if (data == null) {
            return;
        }
        FilterRow row = new FilterRow();
        filterRows.add(row);
        filterBox.getChildren().add(row);
        refreshPlot();
    }

    private ScrollPane buildDetailsPanel() {
        detailsBox.getStyleClass().add(CONTROLS_STYLE);
        detailsBox.setPrefWidth(RIGHT_PANEL_WIDTH);
        showDetails(-1);
        ScrollPane scroll = new ScrollPane(detailsBox);
        scroll.getStyleClass().add(SCROLL_STYLE);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefWidth(RIGHT_PANEL_WIDTH);
        return scroll;
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open CSV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) {
            return;
        }
        lastDirectory = file.getParentFile();
        loadFile(file.toPath());
    }

    public void loadFile(Path path) {
        load(path);
    }

    private void load(Path path) {
        try {
            data = CsvData.load(path);
        } catch (IOException e) {
            log.warn("Failed to load CSV {}", path, e);
            fileLabel.setText(path.getFileName() + " (failed to load)");
            return;
        }
        fileLabel.setText(path.getFileName().toString());
        populateAxisCombos();
        filterRows.clear();
        filterBox.getChildren().clear();
        showDetails(-1);
        refreshPlot();
    }

    private void populateAxisCombos() {
        List<String> names = data.getHeaders();
        List<String> items = new ArrayList<>(names.size() + 1);
        items.add(NONE_ITEM);
        items.addAll(names);
        List<String> previous = List.of(
                String.valueOf(xAxisCombo.getValue()),
                String.valueOf(yAxisCombo.getValue()),
                String.valueOf(zAxisCombo.getValue()));
        setComboItems(xAxisCombo, items);
        setComboItems(yAxisCombo, items);
        setComboItems(zAxisCombo, items);
        restoreOrDefault(xAxisCombo, previous.get(0), defaultAxis(names, 0));
        restoreOrDefault(yAxisCombo, previous.get(1), defaultAxis(names, 1));
        restoreOrDefault(zAxisCombo, previous.get(2), defaultZ(names));
    }

    private void setComboItems(ComboBox<String> combo, List<String> names) {
        combo.getItems().setAll(names);
    }

    private void restoreOrDefault(ComboBox<String> combo, String previous, String fallback) {
        if (previous != null && combo.getItems().contains(previous)) {
            combo.getSelectionModel().select(previous);
        } else if (fallback != null) {
            combo.getSelectionModel().select(fallback);
        }
    }

    private String defaultAxis(List<String> names, int skip) {
        List<String> varying = names.stream()
                .filter(name -> hasVariation(data.columnIndex(name)))
                .toList();
        return varying.size() > skip ? varying.get(skip)
                : names.size() > skip ? names.get(skip) : null;
    }

    private String defaultZ(List<String> names) {
        for (int i = names.size() - 1; i >= 0; i--) {
            String name = names.get(i);
            int index = data.columnIndex(name);
            if (!name.equals(xAxisCombo.getValue())
                    && !name.equals(yAxisCombo.getValue())
                    && data.isNumeric(index) && hasVariation(index)) {
                return name;
            }
        }
        return NONE_ITEM;
    }

    private boolean hasVariation(int columnIndex) {
        String first = null;
        for (int r = 0; r < data.rowCount(); r++) {
            String cell = data.cell(r, columnIndex);
            if (cell.isBlank()) {
                continue;
            }
            if (first == null) {
                first = cell;
            } else if (!cell.equals(first)) {
                return true;
            }
        }
        return false;
    }

    private void refreshLegend() {
        String x = selectedOrDash(xAxisCombo);
        String y = selectedOrDash(yAxisCombo);
        String z = selectedOrDash(zAxisCombo);
        legendLabel.setText("X: " + x + "    Y: " + y + "    Z: " + z);
        legendLabel.setVisible(data != null);
        legendLabel.getStyleClass().add(LEGEND_STYLE);
        if (!centerPane.getChildren().contains(legendLabel)) {
            legendLabel.setMouseTransparent(true);
            StackPane.setAlignment(legendLabel, Pos.TOP_LEFT);
            centerPane.getChildren().add(legendLabel);
        }
    }

    private String selectedOrDash(ComboBox<String> combo) {
        String value = combo.getValue();
        return value != null && !NONE_ITEM.equals(value) ? value : "-";
    }

    /**
     * Three selected axes plot a 3D surface; exactly two plot a line chart
     * whose horizontal axis is X when selected, otherwise Y, and whose
     * vertical axis is Z when selected, otherwise the remaining axis.
     */
    private void refreshPlot() {
        if (data == null) {
            return;
        }
        AxisMapping x = axisMapping(xAxisCombo);
        AxisMapping y = axisMapping(yAxisCombo);
        AxisMapping z = axisMapping(zAxisCombo);
        int pointCount;
        if (x != null && y != null && z != null) {
            pointCount = showSurface(x, y, z);
        } else if ((x != null ? 1 : 0) + (y != null ? 1 : 0) + (z != null ? 1 : 0) == 2) {
            pointCount = showLine(x != null ? x : y, z != null ? z : y);
        } else {
            pointCount = showEmpty();
        }
        int filtered = countFilteredRows();
        statsLabel.setText(pointCount + " points · " + filtered + "/" + data.rowCount() + " rows");
    }

    private int showSurface(AxisMapping x, AxisMapping y, AxisMapping z) {
        List<SurfacePoint> points = aggregate(x, y, z);
        surfacePane.setData(points, x.scale(), y.scale(), z.scale());
        surfacePane.setVisible(true);
        linePane.setVisible(false);
        placeholder.setVisible(false);
        return points.size();
    }

    private int showLine(AxisMapping h, AxisMapping v) {
        List<Line2DPane.Point> points = aggregate2d(h, v);
        linePane.setData(points, h.scale(), v.scale(), h.name(), v.name());
        linePane.setVisible(true);
        surfacePane.setVisible(false);
        placeholder.setVisible(false);
        return points.size();
    }

    private int showEmpty() {
        surfacePane.setVisible(false);
        linePane.setVisible(false);
        placeholder.setText(SELECT_COLUMNS_TEXT);
        placeholder.setVisible(true);
        return 0;
    }

    /**
     * Resolves a combo selection to an axis mapping: numeric columns are used
     * as-is; categorical columns map each distinct value to an integer
     * position labeled by the {@link AxisScale}. An unselected or
     * {@code NONE_ITEM} combo maps to null.
     */
    private AxisMapping axisMapping(ComboBox<String> combo) {
        String name = combo.getValue();
        if (name == null || NONE_ITEM.equals(name)) {
            return null;
        }
        int column = data.columnIndex(name);
        if (column < 0) {
            return null;
        }
        if (data.isNumeric(column)) {
            return new AxisMapping(column, name, List.of(), null);
        }
        List<String> categories = data.distinctValues(column);
        Map<String, Double> positions = new HashMap<>();
        for (int i = 0; i < categories.size(); i++) {
            positions.put(categories.get(i), (double) i);
        }
        return new AxisMapping(column, name, categories, positions);
    }

    private int countFilteredRows() {
        int count = 0;
        for (int r = 0; r < data.rowCount(); r++) {
            if (passesFilters(r)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Groups filtered rows by their (x, y) position; points sharing a position
     * are averaged and the row with the highest Z value is kept as the
     * representative reading.
     */
    private List<SurfacePoint> aggregate(AxisMapping xm, AxisMapping ym, AxisMapping zm) {
        List<SurfacePoint> points = new ArrayList<>();
        if (xm == null || ym == null || zm == null) {
            return points;
        }
        Map<String, Aggregate> groups = new LinkedHashMap<>();
        for (int r = 0; r < data.rowCount(); r++) {
            if (!passesFilters(r)) {
                continue;
            }
            double x = xm.value(data, r);
            double y = ym.value(data, r);
            double z = zm.value(data, r);
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
                continue;
            }
            groups.computeIfAbsent(x + "|" + y, k -> new Aggregate(x, y))
                    .add(r, z);
        }
        for (Aggregate agg : groups.values()) {
            points.add(agg.toPoint());
        }
        return points;
    }

    /**
     * Two-axis variant of {@link #aggregate}: rows are grouped by their
     * horizontal value only, averaged, and returned sorted along that axis.
     */
    private List<Line2DPane.Point> aggregate2d(AxisMapping hm, AxisMapping vm) {
        Map<Double, Aggregate> groups = new TreeMap<>();
        for (int r = 0; r < data.rowCount(); r++) {
            if (!passesFilters(r)) {
                continue;
            }
            double h = hm.value(data, r);
            double v = vm.value(data, r);
            if (Double.isNaN(h) || Double.isNaN(v)) {
                continue;
            }
            groups.computeIfAbsent(h, k -> new Aggregate(h, 0)).add(r, v);
        }
        List<Line2DPane.Point> points = new ArrayList<>();
        for (Aggregate agg : groups.values()) {
            SurfacePoint p = agg.toPoint();
            points.add(new Line2DPane.Point(p.rowIndex(), p.x(), p.z()));
        }
        return points;
    }

    private boolean passesFilters(int rowIndex) {
        for (FilterRow row : filterRows) {
            if (!row.isActive()) {
                continue;
            }
            if (!row.test(data, rowIndex)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Shows every column of the selected row. Called with {@code -1} to clear.
     */
    private void showDetails(int rowIndex) {
        detailsBox.getChildren().clear();
        if (data == null || rowIndex < 0) {
            Label none = new Label("Click a point on the surface to inspect the row");
            none.getStyleClass().add(STATS_LABEL_STYLE);
            none.setWrapText(true);
            detailsBox.getChildren().add(none);
            return;
        }
        List<Integer> columns = IntStream.range(0, data.columnCount()).boxed().toList();
        detailsBox.getChildren().add(detailSection(SECTION_COLUMNS, columns, rowIndex));
    }

    private VBox detailSection(String title, List<Integer> columns, int rowIndex) {
        Label header = new Label(title);
        header.getStyleClass().add(DETAIL_HEADER_STYLE);
        VBox rows = new VBox(2);
        for (int c : columns) {
            String headerName = data.getHeaders().get(c);
            String cellValue = data.cell(rowIndex, c);
            Label name = new Label(headerName);
            name.getStyleClass().add(DETAIL_NAME_STYLE);
            name.setMinWidth(NAME_COLUMN_WIDTH);
            name.setPrefWidth(NAME_COLUMN_WIDTH);
            name.setTooltip(new Tooltip(headerName));
            Label value = new Label(cellValue);
            value.getStyleClass().add(DETAIL_VALUE_STYLE);
            value.setTooltip(new Tooltip(cellValue));
            HBox.setHgrow(value, Priority.ALWAYS);
            HBox row = new HBox(8, name, value);
            rows.getChildren().add(row);
        }
        VBox section = new VBox(2, header, rows);
        section.setPadding(new Insets(6, 0, 0, 0));
        return section;
    }

    private enum Surface3DPaneColor {
        X(Color.web("#569cd6")),
        Y(Color.web("#4ec9b0")),
        Z(Color.web("#ce9178"));

        private final Color color;

        Surface3DPaneColor(Color color) {
            this.color = color;
        }

        Color color() {
            return color;
        }
    }

    /**
     * One axis resolved against the loaded CSV. {@code positions} is null for
     * numeric columns; for categorical columns it maps each distinct cell
     * value to its integer position on the axis.
     */
    private record AxisMapping(int column, String name, List<String> categories,
                               Map<String, Double> positions) {

        private double value(CsvData data, int row) {
            if (positions == null) {
                return data.numericValue(row, column);
            }
            Double position = positions.get(data.cell(row, column));
            return position == null ? Double.NaN : position;
        }

        private AxisScale scale() {
            return new AxisScale(categories);
        }
    }

    private static final class Aggregate {
        private final double x;
        private final double y;
        private double sum;
        private double maxZ = -Double.MAX_VALUE;
        private int bestRow = -1;
        private int count;

        private Aggregate(double x, double y) {
            this.x = x;
            this.y = y;
        }

        private void add(int rowIndex, double z) {
            sum += z;
            count++;
            if (z > maxZ) {
                maxZ = z;
                bestRow = rowIndex;
            }
        }

        private SurfacePoint toPoint() {
            return new SurfacePoint(bestRow, x, y, sum / count);
        }
    }

    /**
     * One editable filter: column + operator + comparison value. Numeric
     * columns offer all comparison operators with a free-text threshold;
     * categorical/boolean columns offer equals/not-equals with a dropdown of
     * the column's distinct values.
     */
    private final class FilterRow extends HBox {

        private static final int VALUE_INDEX = 2;

        private final ComboBox<String> column = new ComboBox<>();
        private final ComboBox<FilterOperator> operator = new ComboBox<>();
        private final TextField numValue = new TextField();
        private final ComboBox<String> catValue = new ComboBox<>();

        private FilterRow() {
            getStyleClass().add(FILTER_ROW_STYLE);
            setAlignment(Pos.CENTER_LEFT);
            column.getItems().setAll(data.getHeaders());
            column.getStyleClass().add(COMBO_STYLE);
            column.setPromptText("Column");
            HBox.setHgrow(column, Priority.ALWAYS);
            column.setMaxWidth(Double.MAX_VALUE);
            operator.getStyleClass().add(COMBO_STYLE);
            numValue.getStyleClass().add(FIELD_STYLE);
            numValue.setPromptText("Value");
            numValue.setPrefWidth(FIELD_WIDTH);
            catValue.getStyleClass().add(COMBO_STYLE);
            catValue.setPromptText("Value");
            catValue.setPrefWidth(FIELD_WIDTH + 20);
            Button remove = new Button(REMOVE);
            remove.getStyleClass().add(REMOVE_STYLE);
            remove.setOnAction(e -> removeSelf());
            column.valueProperty().addListener((obs, o, n) -> {
                configureForColumn(n);
                refreshPlot();
            });
            operator.valueProperty().addListener((obs, o, n) -> refreshPlot());
            numValue.textProperty().addListener((obs, o, n) -> refreshPlot());
            catValue.valueProperty().addListener((obs, o, n) -> refreshPlot());
            getChildren().addAll(column, operator, numValue, remove);
        }

        private void configureForColumn(String columnName) {
            int index = data.columnIndex(columnName);
            boolean numeric = index >= 0 && data.isNumeric(index);
            FilterOperator selected = operator.getValue();
            operator.getItems().setAll(numeric
                    ? List.of(FilterOperator.values())
                    : List.of(FilterOperator.EQUALS, FilterOperator.NOT_EQUALS));
            operator.getSelectionModel().select(
                    selected != null && operator.getItems().contains(selected)
                            ? selected
                            : numeric ? FilterOperator.GREATER_OR_EQUAL : FilterOperator.EQUALS);
            if (numeric) {
                getChildren().set(VALUE_INDEX, numValue);
            } else {
                catValue.getItems().setAll(data.distinctValues(index));
                getChildren().set(VALUE_INDEX, catValue);
            }
        }

        private boolean isActive() {
            if (column.getValue() == null || operator.getValue() == null) {
                return false;
            }
            int index = data.columnIndex(column.getValue());
            return data.isNumeric(index)
                    ? !Double.isNaN(CsvData.parseNumber(numValue.getText()))
                    : catValue.getValue() != null;
        }

        private boolean test(CsvData csv, int rowIndex) {
            int index = csv.columnIndex(column.getValue());
            if (csv.isNumeric(index)) {
                double cell = csv.numericValue(rowIndex, index);
                double target = CsvData.parseNumber(numValue.getText());
                return !Double.isNaN(cell)
                        && operator.getValue().test(cell, target);
            }
            String cell = csv.cell(rowIndex, index);
            String target = catValue.getValue();
            return switch (operator.getValue()) {
                case EQUALS -> cell.equals(target);
                case NOT_EQUALS -> !cell.equals(target);
                default -> true;
            };
        }

        private void removeSelf() {
            filterRows.remove(this);
            filterBox.getChildren().remove(this);
            refreshPlot();
        }
    }

}

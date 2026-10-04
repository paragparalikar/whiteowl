package com.whiteowl.workbench.surface;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.IntConsumer;

/**
 * Interactive 3D surface plot. Each point is rendered as a quad cell on the
 * X/Y base plane; the edges of every cell extend to the midpoints of its grid
 * neighbours and corner heights are averaged across the points sharing each
 * corner, so adjacent cells form a continuous surface. Cells are colored on a
 * blue-to-red gradient by Z value.
 *
 * <p>Interactions: left-drag rotates, right/middle-drag pans, wheel zooms,
 * hover highlights a cell and click selects it.</p>
 */
public final class Surface3DPane extends StackPane {

    private static final double SPAN = 170;
    private static final double HEIGHT = 140;
    private static final double ROD_THICKNESS = 1.1;
    private static final double DEFAULT_HALF_GAP = 18;
    private static final double ROTATE_SPEED = 0.5;
    private static final double ZOOM_IN = 1.1;
    private static final double ZOOM_OUT = 0.9;
    private static final double MIN_ZOOM = 0.15;
    private static final double MAX_ZOOM = 30;
    private static final double INITIAL_ZOOM = 0.5;
    private static final double CLICK_TOLERANCE = 4;
    private static final double CAMERA_DISTANCE = -620;
    private static final double INITIAL_ROT_X = -32;
    private static final double INITIAL_ROT_Y = 38;
    private static final int TICK_COUNT = 6;
    private static final double TICK_FONT_SIZE = 12;
    private static final String HINT_STYLE = "sa-hint";
    private static final String LEGEND_STYLE = "sa-legend-label";
    private static final String HINT_TEXT = "Left-drag: rotate   Right-drag: pan   Scroll: zoom";

    private static final Color SURFACE_BG = Color.web("#1e1f22");
    private static final Color GRID_COLOR = Color.web("#3c3f41");
    private static final Color AXIS_X_COLOR = Color.web("#569cd6");
    private static final Color AXIS_Y_COLOR = Color.web("#4ec9b0");
    private static final Color AXIS_Z_COLOR = Color.web("#ce9178");
    private static final Color TICK_COLOR = Color.web("#8b949e");
    private static final Color HOVER_COLOR = Color.web("#e6edf3");
    private static final Color SELECT_COLOR = Color.web("#ffd866");
    private static final Color SPECULAR = Color.rgb(50, 50, 55);

    private static final Color[] GRADIENT = {
            Color.web("#2d5c88"),
            Color.web("#4ec9b0"),
            Color.web("#d7ba7d"),
            Color.web("#ef5350")
    };

    private final Group world = new Group();
    private final Group panGroup = new Group(world);
    private final Rotate rotateX = new Rotate(INITIAL_ROT_X, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(INITIAL_ROT_Y, Rotate.Y_AXIS);
    private final Scale zoom = new Scale();
    private final Translate pan = new Translate();
    private final List<Cell> cells = new ArrayList<>();
    private final List<TickLabel> tickLabels = new ArrayList<>();
    private final HBox zLegend = new HBox(6);

    private IntConsumer onPointSelected;
    private Cell hovered;
    private Cell selected;
    private double pressX;
    private double pressY;
    private boolean dragged;
    private MouseButton dragButton = MouseButton.PRIMARY;

    public Surface3DPane() {
        Group root3d = new Group(panGroup);
        AmbientLight ambient = new AmbientLight(Color.rgb(190, 190, 200));
        PointLight keyLight = new PointLight(Color.WHITE);
        keyLight.setTranslateX(250);
        keyLight.setTranslateY(-450);
        keyLight.setTranslateZ(-500);
        root3d.getChildren().addAll(ambient, keyLight);
        SubScene subScene = new SubScene(root3d, 400, 300, true, SceneAntialiasing.BALANCED);
        subScene.setFill(SURFACE_BG);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(CAMERA_DISTANCE);
        camera.setNearClip(0.1);
        camera.setFarClip(8000);
        subScene.setCamera(camera);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        world.getTransforms().addAll(rotateY, rotateX, zoom);
        panGroup.getTransforms().add(pan);
        rotateX.angleProperty().addListener((o, a, b) -> updateTickTransforms());
        rotateY.angleProperty().addListener((o, a, b) -> updateTickTransforms());
        zoom.xProperty().addListener((o, a, b) -> updateTickTransforms());
        installMouseHandlers(subScene);
        Label hint = new Label(HINT_TEXT);
        hint.getStyleClass().add(HINT_STYLE);
        hint.setMouseTransparent(true);
        StackPane.setAlignment(hint, Pos.BOTTOM_LEFT);
        zLegend.setMouseTransparent(true);
        zLegend.setAlignment(Pos.CENTER);
        zLegend.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        zLegend.setPadding(new Insets(0, 10, 0, 0));
        StackPane.setAlignment(zLegend, Pos.CENTER_RIGHT);
        getChildren().addAll(subScene, hint, zLegend);
        resetView();
    }

    public void setOnPointSelected(IntConsumer listener) {
        this.onPointSelected = listener;
    }

    public void clearSelection() {
        select(null);
    }

    /**
     * Rebuilds the surface. X/Y values are normalized onto the base plane and Z
     * values onto the surface height.
     */
    public void setData(List<SurfacePoint> points) {
        world.getChildren().clear();
        cells.clear();
        tickLabels.clear();
        zLegend.getChildren().clear();
        hovered = null;
        selected = null;
        if (points.isEmpty()) {
            return;
        }
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (SurfacePoint p : points) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
            minZ = Math.min(minZ, p.z());
            maxZ = Math.max(maxZ, p.z());
        }
        addAxesAndGrid();
        addTicks(minX, maxX, minY, maxY);
        buildZLegend(minZ, maxZ);
        double[] xs = distinctAxisPositions(points, true, minX, maxX);
        double[] ys = distinctAxisPositions(points, false, minY, maxY);
        Map<Long, SurfacePoint> grid = new HashMap<>();
        Map<Double, Integer> xIndex = indexMap(points, true);
        Map<Double, Integer> yIndex = indexMap(points, false);
        for (SurfacePoint p : points) {
            grid.put((long) xIndex.get(p.x()) * ys.length + yIndex.get(p.y()), p);
        }
        for (SurfacePoint p : points) {
            int ix = xIndex.get(p.x());
            int iy = yIndex.get(p.y());
            double[] corners = cornerHeights(grid, ix, iy, xs.length, ys.length, minZ, maxZ);
            MeshView quad = createQuad(
                    edgeLeft(xs, ix), edgeRight(xs, ix),
                    edgeLeft(ys, iy), edgeRight(ys, iy), corners);
            Color base = gradient(normalizeHeight(p.z(), minZ, maxZ));
            PhongMaterial material = new PhongMaterial(base);
            material.setSpecularColor(SPECULAR);
            quad.setMaterial(material);
            quad.setCullFace(CullFace.NONE);
            Cell cell = new Cell(quad, material, base, p.rowIndex());
            quad.setUserData(cell);
            cells.add(cell);
            world.getChildren().add(quad);
        }
    }

    public void resetView() {
        rotateX.setAngle(INITIAL_ROT_X);
        rotateY.setAngle(INITIAL_ROT_Y);
        zoom.setX(INITIAL_ZOOM);
        zoom.setY(INITIAL_ZOOM);
        zoom.setZ(INITIAL_ZOOM);
        pan.setX(0);
        pan.setY(0);
        pan.setZ(0);
        updateTickTransforms();
    }

    /**
     * Heights of a cell's four corners as world Y values. A corner shared by
     * several grid points averages their Z values so adjacent cells meet at
     * exactly the same height.
     */
    private double[] cornerHeights(Map<Long, SurfacePoint> grid, int ix, int iy,
                                   int nx, int ny, double minZ, double maxZ) {
        return new double[]{
                cornerHeight(grid, ix - 1, iy - 1, nx, ny, minZ, maxZ),
                cornerHeight(grid, ix, iy - 1, nx, ny, minZ, maxZ),
                cornerHeight(grid, ix, iy, nx, ny, minZ, maxZ),
                cornerHeight(grid, ix - 1, iy, nx, ny, minZ, maxZ)
        };
    }

    private double cornerHeight(Map<Long, SurfacePoint> grid, int i0, int j0,
                                int nx, int ny, double minZ, double maxZ) {
        double sum = 0;
        int count = 0;
        for (int i = i0; i <= i0 + 1; i++) {
            for (int j = j0; j <= j0 + 1; j++) {
                if (i < 0 || j < 0 || i >= nx || j >= ny) {
                    continue;
                }
                SurfacePoint p = grid.get((long) i * ny + j);
                if (p != null) {
                    sum += p.z();
                    count++;
                }
            }
        }
        double z = count > 0 ? sum / count : minZ;
        return -normalizeHeight(z, minZ, maxZ) * HEIGHT;
    }

    private MeshView createQuad(double xL, double xR, double zT, double zB, double[] h) {
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(
                (float) xL, (float) h[0], (float) zT,
                (float) xR, (float) h[1], (float) zT,
                (float) xR, (float) h[2], (float) zB,
                (float) xL, (float) h[3], (float) zB);
        mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
        mesh.getFaces().addAll(0, 0, 1, 1, 2, 2, 0, 0, 2, 2, 3, 3);
        return new MeshView(mesh);
    }

    private static double edgeLeft(double[] positions, int index) {
        if (index > 0) {
            return (positions[index - 1] + positions[index]) / 2;
        }
        double gap = positions.length > 1 ? positions[1] - positions[0] : DEFAULT_HALF_GAP * 2;
        return positions[index] - gap / 2;
    }

    private static double edgeRight(double[] positions, int index) {
        if (index < positions.length - 1) {
            return (positions[index] + positions[index + 1]) / 2;
        }
        double gap = positions.length > 1
                ? positions[positions.length - 1] - positions[positions.length - 2]
                : DEFAULT_HALF_GAP * 2;
        return positions[index] + gap / 2;
    }

    private double[] distinctAxisPositions(List<SurfacePoint> points, boolean xAxis,
                                           double min, double max) {
        TreeSet<Double> values = new TreeSet<>();
        for (SurfacePoint p : points) {
            values.add(xAxis ? p.x() : p.y());
        }
        return values.stream().mapToDouble(v -> normalize(v, min, max) * SPAN).toArray();
    }

    private Map<Double, Integer> indexMap(List<SurfacePoint> points, boolean xAxis) {
        TreeSet<Double> values = new TreeSet<>();
        for (SurfacePoint p : points) {
            values.add(xAxis ? p.x() : p.y());
        }
        Map<Double, Integer> map = new HashMap<>();
        int i = 0;
        for (double v : values) {
            map.put(v, i++);
        }
        return map;
    }

    private void installMouseHandlers(SubScene subScene) {
        subScene.setOnMousePressed(e -> {
            pressX = e.getSceneX();
            pressY = e.getSceneY();
            dragged = false;
            dragButton = e.getButton();
        });
        subScene.setOnMouseDragged(e -> handleDrag(e));
        subScene.setOnScroll(this::handleScroll);
        subScene.setOnMouseMoved(e -> updateHover(e.getPickResult()));
        subScene.setOnMouseExited(e -> updateHover(null));
        subScene.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && !dragged) {
                Cell cell = cellAt(e.getPickResult());
                select(cell == selected ? null : cell);
            }
        });
        subScene.setOnMouseReleased(e -> getScene().setCursor(Cursor.DEFAULT));
    }

    private void handleDrag(MouseEvent e) {
        double dx = e.getSceneX() - pressX;
        double dy = e.getSceneY() - pressY;
        if (Math.abs(dx) + Math.abs(dy) > CLICK_TOLERANCE) {
            dragged = true;
        }
        if (!dragged) {
            return;
        }
        if (dragButton == MouseButton.PRIMARY) {
            rotateY.setAngle(rotateY.getAngle() - dx * ROTATE_SPEED);
            rotateX.setAngle(rotateX.getAngle() + dy * ROTATE_SPEED);
            getScene().setCursor(Cursor.CLOSED_HAND);
        } else {
            pan.setX(pan.getX() + dx);
            pan.setY(pan.getY() + dy);
            getScene().setCursor(Cursor.MOVE);
        }
        pressX = e.getSceneX();
        pressY = e.getSceneY();
        updateHover(e.getPickResult());
    }

    private void handleScroll(ScrollEvent e) {
        double factor = e.getDeltaY() > 0 ? ZOOM_OUT : ZOOM_IN;
        double scale = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, zoom.getX() * factor));
        zoom.setX(scale);
        zoom.setY(scale);
        zoom.setZ(scale);
    }

    private void updateHover(PickResult pick) {
        setHovered(cellAt(pick));
    }

    private Cell cellAt(PickResult pick) {
        if (pick == null) {
            return null;
        }
        Node node = pick.getIntersectedNode();
        return node != null && node.getUserData() instanceof Cell cell ? cell : null;
    }

    private void setHovered(Cell cell) {
        if (hovered == cell) {
            return;
        }
        hovered = cell;
        repaintCells();
    }

    private void select(Cell cell) {
        selected = cell;
        repaintCells();
        if (onPointSelected != null) {
            onPointSelected.accept(cell == null ? -1 : cell.rowIndex);
        }
    }

    private void repaintCells() {
        for (Cell cell : cells) {
            Color color = cell == selected ? SELECT_COLOR
                    : cell == hovered ? HOVER_COLOR
                    : cell.baseColor;
            cell.material.setDiffuseColor(color);
        }
    }

    private void addAxesAndGrid() {
        double s = SPAN;
        addRod(-s, 0, s, s, 0, s, AXIS_X_COLOR);
        addRod(-s, 0, -s, -s, 0, s, AXIS_Y_COLOR);
        addRod(s, 0, -s, s, -HEIGHT, -s, AXIS_Z_COLOR);
        for (int i = 1; i < 4; i++) {
            double t = -s + (2 * s * i / 4);
            addRod(-s, 0, t, s, 0, t, GRID_COLOR);
            addRod(t, 0, -s, t, 0, s, GRID_COLOR);
        }
    }

    private void addTicks(double minX, double maxX, double minY, double maxY) {
        for (int i = 0; i < TICK_COUNT; i++) {
            double t = TICK_COUNT > 1 ? (double) i / (TICK_COUNT - 1) : 0.5;
            addTickLabel(formatTick(minX + t * (maxX - minX)),
                    normalize(minX + t * (maxX - minX), minX, maxX) * SPAN, 24, SPAN + 14);
            addTickLabel(formatTick(minY + t * (maxY - minY)),
                    -SPAN - 26, 14, normalize(minY + t * (maxY - minY), minY, maxY) * SPAN);
        }
        updateTickTransforms();
    }

    /**
     * Z values are shown on a 2D color-scale legend at the right edge: the
     * gradient bar doubles as the color key for the surface cells.
     */
    private void buildZLegend(double minZ, double maxZ) {
        zLegend.getChildren().clear();
        VBox labels = new VBox();
        labels.setAlignment(Pos.CENTER);
        VBox.setVgrow(labels, Priority.ALWAYS);
        labels.setMinHeight(160);
        labels.setSpacing(0);
        for (int i = 0; i < TICK_COUNT; i++) {
            double t = TICK_COUNT > 1 ? (double) i / (TICK_COUNT - 1) : 0.5;
            Label label = new Label(formatTick(maxZ - t * (maxZ - minZ)));
            label.getStyleClass().add(LEGEND_STYLE);
            labels.getChildren().add(label);
            if (i < TICK_COUNT - 1) {
                Region spacer = new Region();
                VBox.setVgrow(spacer, Priority.ALWAYS);
                labels.getChildren().add(spacer);
            }
        }
        Rectangle bar = new Rectangle(12, 160);
        Stop[] stops = new Stop[GRADIENT.length];
        for (int i = 0; i < GRADIENT.length; i++) {
            stops[i] = new Stop(1.0 - (double) i / (GRADIENT.length - 1),
                    GRADIENT[GRADIENT.length - 1 - i]);
        }
        bar.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops));
        zLegend.getChildren().addAll(labels, bar);
    }

    /**
     * Tick labels live in world space but counter-rotate and counter-scale so
     * they always face the camera at a constant on-screen size.
     */
    private void addTickLabel(String text, double x, double y, double z) {
        Text label = new Text(text);
        label.setFill(TICK_COLOR);
        label.setFont(Font.font(TICK_FONT_SIZE));
        label.setMouseTransparent(true);
        Scale invScale = new Scale();
        Rotate invRotX = new Rotate(0, Rotate.X_AXIS);
        Rotate invRotY = new Rotate(0, Rotate.Y_AXIS);
        label.getTransforms().addAll(invScale, new Translate(x, y, z), invRotX, invRotY);
        tickLabels.add(new TickLabel(invScale, invRotX, invRotY));
        world.getChildren().add(label);
    }

    private void updateTickTransforms() {
        double inv = 1.0 / zoom.getX();
        for (TickLabel label : tickLabels) {
            label.invScale.setX(inv);
            label.invScale.setY(inv);
            label.invRotX.setAngle(-rotateX.getAngle());
            label.invRotY.setAngle(-rotateY.getAngle());
        }
    }

    private void addRod(double x1, double y1, double z1,
                        double x2, double y2, double z2, Color color) {
        double dx = Math.abs(x2 - x1);
        double dy = Math.abs(y2 - y1);
        double dz = Math.abs(z2 - z1);
        Box rod = new Box(Math.max(dx, ROD_THICKNESS),
                Math.max(dy, ROD_THICKNESS), Math.max(dz, ROD_THICKNESS));
        rod.setTranslateX((x1 + x2) / 2);
        rod.setTranslateY((y1 + y2) / 2);
        rod.setTranslateZ((z1 + z2) / 2);
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecularColor(Color.BLACK);
        rod.setMaterial(material);
        world.getChildren().add(rod);
    }

    private static double normalize(double value, double min, double max) {
        if (max <= min) {
            return 0;
        }
        return (value - min) / (max - min) * 2 - 1;
    }

    private static double normalizeHeight(double value, double min, double max) {
        if (max <= min) {
            return 0.5;
        }
        return (value - min) / (max - min);
    }

    private static String formatTick(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        if (Math.abs(value) >= 1000) {
            return String.format("%,.0f", value);
        }
        return String.format("%.4g", value);
    }

    private static Color gradient(double t) {
        double clamped = Math.min(1, Math.max(0, t));
        double scaled = clamped * (GRADIENT.length - 1);
        int i = Math.min((int) scaled, GRADIENT.length - 2);
        return GRADIENT[i].interpolate(GRADIENT[i + 1], scaled - i);
    }

    private record TickLabel(Scale invScale, Rotate invRotX, Rotate invRotY) {
    }

    private static final class Cell {
        private final Node node;
        private final PhongMaterial material;
        private final Color baseColor;
        private final int rowIndex;

        private Cell(Node node, PhongMaterial material, Color baseColor, int rowIndex) {
            this.node = node;
            this.material = material;
            this.baseColor = baseColor;
            this.rowIndex = rowIndex;
        }
    }

}

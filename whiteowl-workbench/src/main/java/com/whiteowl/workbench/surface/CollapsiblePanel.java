package com.whiteowl.workbench.surface;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.fluentui.FluentUiRegularAL;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Wraps a side panel with a slim edge strip holding a caret button. Clicking
 * the caret collapses the panel down to just the strip (inside a SplitPane it
 * snaps to the edge); clicking again restores it to its previous width.
 */
public final class CollapsiblePanel extends BorderPane {

    private static final double STRIP_WIDTH = 20;
    private static final double MIN_EXPANDED_WIDTH = 120;
    private static final String STRIP_STYLE = "sa-collapse-strip";

    /** Which window edge the panel collapses toward. */
    public enum Side {LEFT, RIGHT}

    private final Node content;
    private final Side side;
    private final Button strip;
    private final FontIcon icon;
    private boolean collapsed;
    private double lastExpandedWidth;

    public CollapsiblePanel(Node content, Side side, double expandedWidth) {
        this.content = content;
        this.side = side;
        this.lastExpandedWidth = expandedWidth;
        this.icon = new FontIcon();
        this.icon.setIconSize(10);
        this.strip = new Button();
        strip.setGraphic(icon);
        strip.getStyleClass().add(STRIP_STYLE);
        strip.setMaxHeight(Double.MAX_VALUE);
        strip.setFocusTraversable(false);
        strip.setOnAction(e -> toggle());
        setMinWidth(MIN_EXPANDED_WIDTH);
        setPrefWidth(expandedWidth);
        setCenter(content);
        if (side == Side.LEFT) {
            setRight(strip);
        } else {
            setLeft(strip);
        }
        updateIcon();
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void toggle() {
        collapsed = !collapsed;
        if (collapsed) {
            collapse();
        } else {
            expand();
        }
        updateIcon();
    }

    private void collapse() {
        if (getWidth() > STRIP_WIDTH + 4) {
            lastExpandedWidth = getWidth();
        }
        content.setManaged(false);
        content.setVisible(false);
        setMinWidth(STRIP_WIDTH);
        setPrefWidth(STRIP_WIDTH);
        setMaxWidth(STRIP_WIDTH);
    }

    private void expand() {
        setMaxWidth(Region.USE_COMPUTED_SIZE);
        setMinWidth(MIN_EXPANDED_WIDTH);
        setPrefWidth(lastExpandedWidth);
        content.setManaged(true);
        content.setVisible(true);
        restoreDivider();
    }

    private void restoreDivider() {
        Parent parent = getParent();
        if (!(parent instanceof SplitPane splitPane)) {
            return;
        }
        int index = splitPane.getItems().indexOf(this);
        if (index < 0 || splitPane.getWidth() <= 0) {
            return;
        }
        double fraction = lastExpandedWidth / splitPane.getWidth();
        splitPane.setDividerPosition(index, side == Side.LEFT ? fraction : 1 - fraction);
    }

    private void updateIcon() {
        boolean pointRight = side == Side.LEFT ? collapsed : !collapsed;
        icon.setIconCode(pointRight ? FluentUiRegularAL.CARET_RIGHT_16 : FluentUiRegularAL.CARET_LEFT_16);
        strip.setAlignment(Pos.CENTER);
    }

}

package com.whiteowl.workbench.charting;

import javafx.scene.paint.Color;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChartTheme {

    public static final Color BACKGROUND = Color.web("#1e1f22");
    public static final Color BULLISH = Color.web("#26a69a");
    public static final Color BEARISH = Color.web("#ef5350");
    public static final Color GRID = Color.web("#2b2d30");
    public static final Color AXIS_TEXT = Color.web("#868a91");
    public static final Color AXIS_MAJOR_TEXT = Color.web("#bbbbbb");
    public static final Color CROSSHAIR = Color.web("#555555");
    public static final Color CROSSHAIR_LABEL_BG = Color.web("#2d5c88");
    public static final Color CROSSHAIR_LABEL_TEXT = Color.web("#ffffff");
    public static final Color INFO_LABEL = Color.web("#868a91");
    public static final Color INFO_VALUE = Color.web("#bbbbbb");
    public static final Color[] INDICATOR_PALETTE = {
            Color.web("#f0b90b"),
            Color.web("#2196f3"),
            Color.web("#e040fb"),
            Color.web("#00bcd4"),
            Color.web("#ff9800"),
            Color.web("#4caf50"),
            Color.web("#ff5722"),
            Color.web("#9c27b0")
    };
    public static final double PRICE_AXIS_WIDTH = 40;
    public static final double TIME_AXIS_HEIGHT = 16;
    public static final double INFO_BAR_HEIGHT = 18;
    public static final double PADDING_TOP = 28;
    public static final double PADDING_BOTTOM = 4;
    public static final double WICK_WIDTH = 1;
    public static final double AXIS_FONT_SIZE = 9;
    public static final double INFO_FONT_SIZE = 11;
    public static final double VOLUME_ZONE_RATIO = 0.20;
    public static final double VOLUME_OPACITY = 0.25;
    public static final Color SCREEN_MARKER = Color.web("#4caf50");
    public static final double SCREEN_MARKER_SIZE = 6;
    public static final double SCREEN_MARKER_GAP = 3;
    public static final Color TRADE_WIN = Color.web("#4ec9b0", 0.30);
    public static final Color TRADE_LOSS = Color.web("#ef5350", 0.30);
    public static final Color TRADE_BREAKEVEN = Color.web("#6897bb", 0.30);
    public static final Color TRADE_WIN_STROKE = Color.web("#4ec9b0", 0.60);
    public static final Color TRADE_LOSS_STROKE = Color.web("#ef5350", 0.60);
    public static final Color TRADE_BREAKEVEN_STROKE = Color.web("#6897bb", 0.60);
    public static final double TRADE_MARKER_SIZE = 5;
    public static final double TRADE_STROKE_WIDTH = 1.0;
    public static final float TRADE_BREAKEVEN_THRESHOLD = 0.01f;
    public static final Color TRENDLINE_RESISTANCE = Color.web("#ef5350", 0.80);
    public static final Color TRENDLINE_SUPPORT = Color.web("#26a69a", 0.80);
    public static final Color TRENDLINE_TOUCH = Color.web("#f0b90b", 0.70);
    public static final double TRENDLINE_WIDTH = 1.5;
    public static final double TRENDLINE_TOUCH_RADIUS = 3.0;
    public static final double[] TRENDLINE_DASH = {8, 4};
    public static final Color VOLUME_PROFILE_FILL = Color.web("#2196f3", 0.25);
    public static final Color VOLUME_PROFILE_STROKE = Color.web("#2196f3", 0.50);
    public static final Color VOLUME_PROFILE_TEXT = Color.web("#bbbbbb", 0.80);
    public static final Color VOLUME_PROFILE_MAX_FILL = Color.web("#f0b90b", 0.30);
    public static final Color VOLUME_PROFILE_MAX_STROKE = Color.web("#f0b90b", 0.55);
    public static final double VOLUME_PROFILE_STROKE_WIDTH = 1.0;
    public static final double VOLUME_PROFILE_TEXT_SIZE = 9;
    public static final Color GTT_LINE_BUY = Color.web("#2196f3", 0.85);
    public static final Color GTT_LINE_SELL = Color.web("#ef5350", 0.85);
    public static final Color GTT_LINE_BUY_BG = Color.web("#2196f3", 0.15);
    public static final Color GTT_LINE_SELL_BG = Color.web("#ef5350", 0.15);
    public static final double[] GTT_LINE_DASH = {6, 3};
    public static final Color ORDER_LINE_BUY = Color.web("#ff9800", 0.85);
    public static final Color ORDER_LINE_SELL = Color.web("#ab47bc", 0.85);
    public static final Color ORDER_LINE_BUY_BG = Color.web("#ff9800", 0.15);
    public static final Color ORDER_LINE_SELL_BG = Color.web("#ab47bc", 0.15);
    public static final double[] ORDER_LINE_DASH = {3, 2};
    public static final Color ORDER_LINE_LABEL_TEXT = Color.web("#ffffff");
    public static final double ORDER_LINE_WIDTH = 1.0;
    public static final double ORDER_LINE_LABEL_FONT_SIZE = 9;
    public static final double ORDER_LINE_LABEL_HEIGHT = 14;
    public static final double ORDER_LINE_LABEL_PADDING = 4;
    public static final double ORDER_LINE_HIT_RADIUS = 5.0;
    public static final double[] PRICE_PICKER_DASH = {8, 4};
    public static final double PRICE_PICKER_LABEL_FONT_SIZE = 10;

}

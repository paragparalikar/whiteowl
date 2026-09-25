package com.whiteowl.core.backtest.v2.optimization.report;

import com.whiteowl.core.backtest.v2.optimization.plateau.CurvePoint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Locale;

/**
 * Minimal inline-SVG chart builders — no external JS/CSS needed, keeping the
 * report fully self-contained.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Svg {

    private static final int W = 640;
    private static final int H = 220;
    private static final int PAD = 36;

    /**
     * Metric-vs-parameter-value line chart with the plateau region shaded.
     */
    static String sensitivityChart(List<CurvePoint> curve, Double regionLower,
                                    Double regionUpper, String title) {
        if (curve == null || curve.isEmpty()) {
            return "";
        }
        double xMin = curve.get(0).parameterValue();
        double xMax = curve.get(curve.size() - 1).parameterValue();
        double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
        for (CurvePoint p : curve) {
            if (Double.isNaN(p.metric())) continue;
            yMin = Math.min(yMin, p.metric());
            yMax = Math.max(yMax, p.metric());
        }
        if (yMax <= yMin) { yMax = yMin + 1; }
        double xSpan = Math.max(1e-9, xMax - xMin);
        double ySpan = yMax - yMin;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT,
                "<svg viewBox='0 0 %d %d' class='chart'><text x='%d' y='16' class='title'>%s</text>",
                W, H, PAD, esc(title)));

        // plateau shading
        if (regionLower != null && regionUpper != null && !Double.isNaN(regionLower)) {
            double rx = PAD + (regionLower - xMin) / xSpan * (W - 2 * PAD);
            double rw = (regionUpper - regionLower) / xSpan * (W - 2 * PAD);
            sb.append(String.format(Locale.ROOT,
                    "<rect x='%.1f' y='%d' width='%.1f' height='%d' class='plateau'/>",
                    rx, PAD, Math.max(2, rw), H - 2 * PAD));
        }

        // axes
        sb.append(String.format(Locale.ROOT,
                "<line x1='%d' y1='%d' x2='%d' y2='%d' class='axis'/>"
                        + "<line x1='%d' y1='%d' x2='%d' y2='%d' class='axis'/>",
                PAD, H - PAD, W - PAD, H - PAD,
                PAD, PAD, PAD, H - PAD));
        sb.append(String.format(Locale.ROOT,
                "<text x='%d' y='%d' class='lbl'>%s</text>"
                        + "<text x='%d' y='%d' class='lbl'>%s</text>"
                        + "<text x='4' y='%d' class='lbl'>%.2f</text>"
                        + "<text x='4' y='%d' class='lbl'>%.2f</text>",
                PAD, H - 6, fmt(xMin), W - PAD, H - 6, fmt(xMax),
                PAD + 6, yMax, H - PAD, yMin));

        // polyline + points
        StringBuilder poly = new StringBuilder();
        for (CurvePoint p : curve) {
            if (Double.isNaN(p.metric())) continue;
            double x = PAD + (p.parameterValue() - xMin) / xSpan * (W - 2 * PAD);
            double y = H - PAD - (p.metric() - yMin) / ySpan * (H - 2 * PAD);
            poly.append(String.format(Locale.ROOT, "%.1f,%.1f ", x, y));
            sb.append(String.format(Locale.ROOT,
                    "<circle cx='%.1f' cy='%.1f' r='3' class='pt'/>", x, y));
        }
        sb.append(String.format(Locale.ROOT,
                "<polyline points='%s' class='line'/>", poly));
        sb.append("</svg>");
        return sb.toString();
    }

    /** Grouped bar chart of per-window IS vs OOS metric. */
    static String walkForwardChart(List<double[]> pairs, String title) {
        if (pairs == null || pairs.isEmpty()) {
            return "";
        }
        double yMax = 0;
        for (double[] p : pairs) {
            yMax = Math.max(yMax, Math.max(p[0], p[1]));
        }
        if (yMax <= 0) yMax = 1;
        int n = pairs.size();
        double slot = (W - 2.0 * PAD) / n;
        double bw = slot / 3;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT,
                "<svg viewBox='0 0 %d %d' class='chart'><text x='%d' y='16' class='title'>%s</text>",
                W, H, PAD, esc(title)));
        sb.append(String.format(Locale.ROOT,
                "<line x1='%d' y1='%d' x2='%d' y2='%d' class='axis'/>",
                PAD, H - PAD, W - PAD, H - PAD));
        for (int i = 0; i < n; i++) {
            double[] p = pairs.get(i);
            double x = PAD + i * slot + slot / 4;
            double isH = p[0] / yMax * (H - 2 * PAD);
            double osH = p[1] / yMax * (H - 2 * PAD);
            sb.append(String.format(Locale.ROOT,
                    "<rect x='%.1f' y='%.1f' width='%.1f' height='%.1f' class='is'/>",
                    x, H - PAD - isH, bw, isH));
            sb.append(String.format(Locale.ROOT,
                    "<rect x='%.1f' y='%.1f' width='%.1f' height='%.1f' class='oos'/>",
                    x + bw + 2, H - PAD - osH, bw, osH));
            sb.append(String.format(Locale.ROOT,
                    "<text x='%.1f' y='%d' class='lbl'>w%d</text>",
                    x, H - 6, i));
        }
        sb.append("<text x='").append(W - PAD - 150).append("' y='16' class='lbl'>")
                .append("■ in-sample&nbsp;&nbsp;<tspan class='oosT'>■ out-of-sample</tspan></text>");
        sb.append("</svg>");
        return sb.toString();
    }

    /**
     * Line/area chart of an equity-like series. {@code drawdown=true} renders
     * a red filled drawdown profile; otherwise a blue equity line.
     */
    static String curveChart(float[] values, String title, boolean drawdown) {
        if (values == null || values.length < 2) {
            return "";
        }
        double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
        for (float v : values) {
            yMin = Math.min(yMin, v);
            yMax = Math.max(yMax, v);
        }
        if (yMax <= yMin) yMax = yMin + 1;
        double ySpan = yMax - yMin;
        int n = values.length;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT,
                "<svg viewBox='0 0 %d %d' class='chart'><text x='%d' y='16' class='title'>%s</text>",
                W, H, PAD, esc(title)));
        sb.append(String.format(Locale.ROOT,
                "<line x1='%d' y1='%d' x2='%d' y2='%d' class='axis'/>"
                        + "<line x1='%d' y1='%d' x2='%d' y2='%d' class='axis'/>",
                PAD, H - PAD, W - PAD, H - PAD, PAD, PAD, PAD, H - PAD));
        sb.append(String.format(Locale.ROOT,
                "<text x='4' y='%d' class='lbl'>%.0f</text>"
                        + "<text x='4' y='%d' class='lbl'>%.0f</text>",
                PAD + 6, yMax, H - PAD, yMin));
        StringBuilder poly = new StringBuilder();
        for (int i = 0; i < n; i++) {
            double x = PAD + (double) i / (n - 1) * (W - 2 * PAD);
            double y = H - PAD - (values[i] - yMin) / ySpan * (H - 2 * PAD);
            poly.append(String.format(Locale.ROOT, "%.1f,%.1f ", x, y));
        }
        if (drawdown) {
            sb.append(String.format(Locale.ROOT,
                    "<polygon points='%d,%d %s %d,%d' class='ddArea'/>",
                    PAD, H - PAD, poly, W - PAD, H - PAD));
        }
        sb.append(String.format(Locale.ROOT, "<polyline points='%s' class='%s'/>",
                poly, drawdown ? "ddLine" : "line"));
        sb.append("</svg>");
        return sb.toString();
    }

    /** % drawdown series computed from an equity curve. */
    static float[] drawdownSeries(float[] equity) {
        float[] out = new float[equity.length];
        float peak = equity[0];
        for (int i = 0; i < equity.length; i++) {
            peak = Math.max(peak, equity[i]);
            out[i] = peak > 0 ? (peak - equity[i]) / peak * 100f : 0;
        }
        return out;
    }

    /**
     * Color-cell heatmap. {@code values[r][c]} — NaN renders dark grey.
     * Diverging red→dark→green scale centered on the value midrange.
     */
    static String heatmap(List<String> colLabels, List<String> rowLabels,
                           double[][] values, String title) {
        if (values == null || values.length == 0) return "";
        int rows = values.length, cols = values[0].length;
        double vmin = Double.POSITIVE_INFINITY, vmax = Double.NEGATIVE_INFINITY;
        for (double[] row : values) {
            for (double v : row) {
                if (Double.isNaN(v)) continue;
                vmin = Math.min(vmin, v);
                vmax = Math.max(vmax, v);
            }
        }
        if (vmax <= vmin) vmax = vmin + 1;
        int top = 40, left = 60;
        double cw = (W - left - 10.0) / cols, ch = (H - top - 20.0) / rows;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT,
                "<svg viewBox='0 0 %d %d' class='chart'><text x='%d' y='16' class='title'>%s</text>"
                        + "<text x='%d' y='%d' class='lbl'>%.0f</text>"
                        + "<text x='%d' y='30' class='lbl'>%.0f</text>",
                W, H, PAD, esc(title), W - 30, H - 4, vmin, W - 30, vmax));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double v = values[r][c];
                sb.append(String.format(Locale.ROOT,
                        "<rect x='%.1f' y='%.1f' width='%.1f' height='%.1f' "
                                + "fill='%s'><title>%s = %s</title></rect>",
                        left + c * cw, top + r * ch, Math.max(1, cw - 1),
                        Math.max(1, ch - 1),
                        Double.isNaN(v) ? "#1a2438" : heatColor(v, vmin, vmax),
                        esc(rowLabels.get(r) + "/" + colLabels.get(c)),
                        Double.isNaN(v) ? "n/a" : String.format(Locale.ROOT, "%.2f", v)));
            }
        }
        for (int c = 0; c < cols; c++) {
            sb.append(String.format(Locale.ROOT,
                    "<text x='%.1f' y='%d' class='lbl' text-anchor='middle'>%s</text>",
                    left + c * cw + cw / 2, top - 4, esc(colLabels.get(c))));
        }
        for (int r = 0; r < rows; r++) {
            sb.append(String.format(Locale.ROOT,
                    "<text x='%d' y='%.1f' class='lbl' text-anchor='end'>%s</text>",
                    left - 4, top + r * ch + ch / 2 + 3, esc(rowLabels.get(r))));
        }
        sb.append("</svg>");
        return sb.toString();
    }

    /** Diverging scale: red (low) → dark (mid) → green (high). */
    private static String heatColor(double v, double vmin, double vmax) {
        double t = (v - vmin) / (vmax - vmin); // 0..1
        int r, g, b;
        if (t < 0.5) {          // red → dark slate
            double k = t * 2;
            r = (int) (0xc0 + (0x1a - 0xc0) * k);
            g = (int) (0x39 + (0x24 - 0x39) * k);
            b = (int) (0x39 + (0x38 - 0x39) * k);
        } else {                // dark slate → green
            double k = (t - 0.5) * 2;
            r = (int) (0x1a + (0x2e - 0x1a) * k);
            g = (int) (0x24 + (0xa0 - 0x24) * k);
            b = (int) (0x38 + (0x59 - 0x38) * k);
        }
        return String.format(Locale.ROOT, "#%02x%02x%02x", r, g, b);
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String fmt(double v) {
        return v == Math.rint(v) && Math.abs(v) < 1e9
                ? Long.toString((long) v) : String.format(Locale.ROOT, "%.2f", v);
    }

}

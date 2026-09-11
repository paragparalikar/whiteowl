package com.whiteowl.workbench.charting;

import static com.whiteowl.workbench.charting.ChartTheme.*;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeAxisRenderer {

    private static final double CHAR_WIDTH = AXIS_FONT_SIZE * 0.6;
    private static final int MIN_LABEL_GAP_PX = 10;
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter FMT_FULL = DateTimeFormatter.ofPattern("dd MMM yy HH:mm");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd MMM yy");
    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yy");
    private static final DateTimeFormatter FMT_DAY = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter FMT_YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter FMT_MONTH = DateTimeFormatter.ofPattern("MMM");
    private static final int FULL_LABEL_CHARS = 15;
    private static final int DATE_LABEL_CHARS = 9;
    private static final int MONTH_YEAR_CHARS = 6;

    private TimeAxisRenderer() {
    }

    public static void draw(GraphicsContext gc, Bars bars, BarDataProvider dataProvider,
                     int viewStart, int viewEnd, double chartW, double totalH,
                     double barWidth) {
        gc.setFont(Font.font(AXIS_FONT_SIZE));
        double y = totalH - 3;
        int step = computeStep(barWidth);
        LabelStrategy strategy = chooseStrategy(barWidth, step);
        LocalDateTime prevDt = null;
        double lastLabelEndX = -1;
        for (int i = viewStart; i < viewEnd; i += step) {
            int idx = dataProvider.translateIndex(i);
            if (idx < 0 || idx >= bars.size()) continue;
            long ts = bars.getTimestamp(idx);
            LocalDateTime dt = toLocalDateTime(ts);
            LabelInfo info = strategy.resolve(dt, prevDt);
            if (info == null) {
                prevDt = dt;
                continue;
            }
            double centerX = (i - viewStart) * barWidth + barWidth / 2;
            double labelW = info.text.length() * CHAR_WIDTH;
            double lx = centerX - labelW / 2;
            if (lx < lastLabelEndX + MIN_LABEL_GAP_PX && lastLabelEndX >= 0) {
                prevDt = dt;
                continue;
            }
            gc.setFill(info.major ? AXIS_MAJOR_TEXT : AXIS_TEXT);
            gc.fillText(info.text, lx, y);
            lastLabelEndX = lx + labelW;
            prevDt = dt;
        }
    }

    public static String formatForCrosshair(long epochMillis, long timeframeSeconds) {
        LocalDateTime dt = toLocalDateTime(epochMillis);
        if (timeframeSeconds >= 2592000L) return dt.format(FMT_MONTH_YEAR);
        if (timeframeSeconds >= 86400L) return dt.format(FMT_DATE);
        return dt.format(FMT_FULL);
    }

    private static int computeStep(double barWidth) {
        return Math.max(1, (int) (60 / barWidth));
    }

    private static LabelStrategy chooseStrategy(double barWidth, int step) {
        double pixelsPerLabel = barWidth * step;
        double fullWidth = FULL_LABEL_CHARS * CHAR_WIDTH;
        double dateWidth = DATE_LABEL_CHARS * CHAR_WIDTH;
        double monthYearWidth = MONTH_YEAR_CHARS * CHAR_WIDTH;
        if (pixelsPerLabel >= fullWidth + MIN_LABEL_GAP_PX) return new FullStrategy();
        if (pixelsPerLabel >= dateWidth + MIN_LABEL_GAP_PX) return new DateTimeStrategy();
        if (pixelsPerLabel >= monthYearWidth + MIN_LABEL_GAP_PX) return new MonthDayStrategy();
        return new YearMonthStrategy();
    }

    private static LocalDateTime toLocalDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZONE);
    }

    private record LabelInfo(String text, boolean major) {
    }

    private interface LabelStrategy {
        LabelInfo resolve(LocalDateTime current, LocalDateTime previous);
    }

    private static final class FullStrategy implements LabelStrategy {
        @Override
        public LabelInfo resolve(LocalDateTime current, LocalDateTime previous) {
            return new LabelInfo(current.format(FMT_FULL), yearChanged(current, previous));
        }
    }

    private static final class DateTimeStrategy implements LabelStrategy {
        @Override
        public LabelInfo resolve(LocalDateTime current, LocalDateTime previous) {
            if (previous == null || dateChanged(current, previous)) {
                boolean major = yearChanged(current, previous) || monthChanged(current, previous);
                return new LabelInfo(current.format(FMT_DATE), major);
            }
            return new LabelInfo(current.format(FMT_TIME), false);
        }
    }

    private static final class MonthDayStrategy implements LabelStrategy {
        @Override
        public LabelInfo resolve(LocalDateTime current, LocalDateTime previous) {
            if (previous == null || monthChanged(current, previous)) {
                return new LabelInfo(current.format(FMT_MONTH_YEAR), true);
            }
            if (dateChanged(current, previous)) {
                return new LabelInfo(current.format(FMT_DAY), false);
            }
            return new LabelInfo(current.format(FMT_TIME), false);
        }
    }

    private static final class YearMonthStrategy implements LabelStrategy {
        @Override
        public LabelInfo resolve(LocalDateTime current, LocalDateTime previous) {
            if (previous == null || yearChanged(current, previous)) {
                return new LabelInfo(current.format(FMT_YEAR), true);
            }
            if (monthChanged(current, previous)) {
                return new LabelInfo(current.format(FMT_MONTH), false);
            }
            return null;
        }
    }

    private static boolean yearChanged(LocalDateTime current, LocalDateTime previous) {
        return previous == null || current.getYear() != previous.getYear();
    }

    private static boolean monthChanged(LocalDateTime current, LocalDateTime previous) {
        return previous == null || current.getMonthValue() != previous.getMonthValue()
                || current.getYear() != previous.getYear();
    }

    private static boolean dateChanged(LocalDateTime current, LocalDateTime previous) {
        return previous == null || current.getDayOfMonth() != previous.getDayOfMonth()
                || monthChanged(current, previous);
    }

}

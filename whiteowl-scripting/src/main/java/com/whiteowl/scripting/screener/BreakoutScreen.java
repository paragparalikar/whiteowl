package com.whiteowl.scripting.screener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public final class BreakoutScreen implements Screen {

    private static final String NAME = "Breakout";
    private static final String SETTING_PRICE_TYPE = "Price Type (0=Close,1=Open,2=High,3=Low)";
    private static final int DEFAULT_PRICE_TYPE = 0;
    private static final String FILE_NAME = "drawings.json";
    private static final Set<String> LINE_TOOLS = Set.of(
            "SLANT_LINE", "SLANT_SEGMENT", "H_LINE", "H_SEGMENT");

    private static final TypeReference<List<DrawingRecord>> LIST_TYPE = new TypeReference<>() {};

    private final Path drawingsBaseDir;
    private final ObjectMapper objectMapper;
    private int priceType = DEFAULT_PRICE_TYPE;

    public BreakoutScreen() {
        this(Paths.get(System.getProperty("whiteowl.home",
                System.getProperty("user.home") + File.separator + ".whiteowl"), "data", "drawings"));
    }

    public BreakoutScreen(Path drawingsBaseDir) {
        this.drawingsBaseDir = drawingsBaseDir;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<ScreenSetting> getSettings() {
        return List.of(new ScreenSetting(SETTING_PRICE_TYPE, Integer.class, DEFAULT_PRICE_TYPE));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        return Map.of(SETTING_PRICE_TYPE, priceType);
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (SETTING_PRICE_TYPE.equals(name) && value instanceof Number n) {
            priceType = n.intValue();
        }
    }

    @Override
    public boolean matches(Scrip scrip, Bars bars) {
        int size = bars.size();
        if (size < 2) return false;

        List<DrawingRecord> drawings = loadDrawings(scrip.getId());
        if (drawings.isEmpty()) return false;

        int last = size - 1;
        int prev = size - 2;
        long currentTs = bars.getTimestamp(last);
        long prevTs = bars.getTimestamp(prev);

        float currentPrice = getPrice(bars, last);
        float currentLow = bars.getLow(last);
        float prevLow = bars.getLow(prev);

        for (DrawingRecord drawing : drawings) {
            if (!isLineDrawing(drawing)) continue;
            AnchorRecord a1 = drawing.anchor1();
            AnchorRecord a2 = drawing.anchor2();
            if (a1 == null) continue;

            double trendValueAtCurrent = trendlineValue(drawing, a1, a2, currentTs);
            if (Double.isNaN(trendValueAtCurrent)) continue;

            // Check if current bar is within the drawn segment
            if (!isWithinSegment(drawing, a1, a2, currentTs)) continue;

            // Condition 1: current price >= trendline
            if (currentPrice < trendValueAtCurrent) continue;

            // Condition 2: current or previous bar's low <= trendline
            boolean currentLowTouches = currentLow <= trendValueAtCurrent;
            boolean prevLowTouches;
            if (isWithinSegment(drawing, a1, a2, prevTs)) {
                double trendValueAtPrev = trendlineValue(drawing, a1, a2, prevTs);
                prevLowTouches = !Double.isNaN(trendValueAtPrev) && prevLow <= trendValueAtPrev;
            } else {
                prevLowTouches = false;
            }

            if (currentLowTouches || prevLowTouches) {
                return true;
            }
        }

        return false;
    }

    private float getPrice(Bars bars, int index) {
        return switch (priceType) {
            case 1 -> bars.getOpen(index);
            case 2 -> bars.getHigh(index);
            case 3 -> bars.getLow(index);
            default -> bars.getClose(index);
        };
    }

    private boolean isLineDrawing(DrawingRecord drawing) {
        return drawing.tool() != null && LINE_TOOLS.contains(drawing.tool());
    }

    private boolean isWithinSegment(DrawingRecord drawing, AnchorRecord a1, AnchorRecord a2,
                                    long timestamp) {
        if (a2 == null) {
            // H_LINE has only one anchor — it's an infinite horizontal line
            return true;
        }
        long minTs = Math.min(a1.timestamp(), a2.timestamp());
        long maxTs = Math.max(a1.timestamp(), a2.timestamp());
        return timestamp >= minTs && timestamp <= maxTs;
    }

    private double trendlineValue(DrawingRecord drawing, AnchorRecord a1, AnchorRecord a2,
                                  long timestamp) {
        if (a2 == null) {
            // Horizontal line — constant price
            return a1.value();
        }
        long t1 = a1.timestamp();
        long t2 = a2.timestamp();
        if (t1 == t2) {
            // Vertical line or zero-length — not meaningful for price comparison
            return Double.NaN;
        }
        // Linear interpolation
        double fraction = (double) (timestamp - t1) / (t2 - t1);
        return a1.value() + fraction * (a2.value() - a1.value());
    }

    private List<DrawingRecord> loadDrawings(String scripId) {
        String sanitized = scripId.replace(':', File.separatorChar);
        Path path = drawingsBaseDir.resolve(sanitized).resolve(FILE_NAME);
        if (!Files.exists(path)) return Collections.emptyList();
        try {
            return objectMapper.readValue(path.toFile(), LIST_TYPE);
        } catch (IOException e) {
            log.debug("Failed to load drawings for {}: {}", scripId, e.getMessage());
            return Collections.emptyList();
        }
    }

    record AnchorRecord(long timestamp, double value) {}

    record DrawingRecord(
            String tool,
            AnchorRecord anchor1,
            AnchorRecord anchor2,
            AnchorRecord anchor3,
            String text,
            String color,
            int canvasId
    ) {}

}

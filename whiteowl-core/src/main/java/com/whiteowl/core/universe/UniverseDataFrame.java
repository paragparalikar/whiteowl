package com.whiteowl.core.universe;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.util.TimestampSearch;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Date-aligned cross-sectional data structure for all symbols in a universe.
 *
 * <p>The existing codebase is scrip-centric: load one scrip's bars, compute indicators, run strategy.
 * The ORB rotational strategy requires a <strong>date-centric</strong> view: on each date, access all
 * symbols' data simultaneously for cross-sectional ranking.</p>
 *
 * <p>This class holds per-scrip bar data aligned to a common date index. It supports both
 * scrip-centric access (get a single symbol's bars) and date-centric access (get a
 * {@link CrossSection} for one date).</p>
 *
 * <p>Constructed via {@link UniverseLoader}.</p>
 */
@Slf4j
public final class UniverseDataFrame {

    /** All symbol scrip IDs in the universe, sorted for deterministic order. */
    @Getter
    private final String[] symbols;

    /** Common date index (epoch millis), sorted ascending. */
    @Getter
    private final long[] dates;

    /** Per-scrip bar data, keyed by scripId. */
    private final Map<String, Bars> barsBySymbol;

    /**
     * Per-scrip index maps: for each symbol, maps dateIndex -> barIndex in that symbol's Bars.
     * A value of -1 means no data for that symbol on that date.
     */
    private final Map<String, int[]> indexMaps;

    /** Named index data (Nifty 50, Bank Nifty, India VIX, etc.), keyed by scripId. */
    private final Map<String, Bars> indexBars;

    /** Index maps for index data, same structure as symbol index maps. */
    private final Map<String, int[]> indexIndexMaps;

    UniverseDataFrame(String[] symbols, long[] dates,
                      Map<String, Bars> barsBySymbol, Map<String, int[]> indexMaps,
                      Map<String, Bars> indexBars, Map<String, int[]> indexIndexMaps) {
        this.symbols = symbols;
        this.dates = dates;
        this.barsBySymbol = barsBySymbol;
        this.indexMaps = indexMaps;
        this.indexBars = indexBars != null ? indexBars : Collections.emptyMap();
        this.indexIndexMaps = indexIndexMaps != null ? indexIndexMaps : Collections.emptyMap();
    }

    /** Number of trading dates in the universe. */
    public int dateCount() {
        return dates.length;
    }

    /** Number of symbols in the universe. */
    public int symbolCount() {
        return symbols.length;
    }

    // --- Scrip-centric access ---

    /**
     * Get the raw Bars for a symbol.
     * Useful for per-scrip indicator computation (e.g., {@code IndicatorFunctions.atr(bars.high, ...)}).
     */
    public Bars getBars(String scripId) {
        return barsBySymbol.get(scripId);
    }

    /**
     * Get index Bars (e.g., Nifty 50, India VIX).
     */
    public Bars getIndexBars(String scripId) {
        return indexBars.get(scripId);
    }

    /** Check if a symbol exists in the universe. */
    public boolean hasSymbol(String scripId) {
        return barsBySymbol.containsKey(scripId);
    }

    // --- Date-centric access ---

    /**
     * Get the close price for a symbol on a given date.
     * @return close price, or {@code Float.NaN} if no data
     */
    public float getClose(String scripId, int dateIndex) {
        return getFloat(scripId, dateIndex, Bars::getClose);
    }

    /** Get the open price for a symbol on a given date. */
    public float getOpen(String scripId, int dateIndex) {
        return getFloat(scripId, dateIndex, Bars::getOpen);
    }

    /** Get the high price for a symbol on a given date. */
    public float getHigh(String scripId, int dateIndex) {
        return getFloat(scripId, dateIndex, Bars::getHigh);
    }

    /** Get the low price for a symbol on a given date. */
    public float getLow(String scripId, int dateIndex) {
        return getFloat(scripId, dateIndex, Bars::getLow);
    }

    /** Get the volume for a symbol on a given date. */
    public long getVolume(String scripId, int dateIndex) {
        int[] map = indexMaps.get(scripId);
        if (map == null || dateIndex < 0 || dateIndex >= dates.length) return 0;
        int barIdx = map[dateIndex];
        if (barIdx < 0) return 0;
        return barsBySymbol.get(scripId).getVolume(barIdx);
    }

    /**
     * Get the bar index in a symbol's raw {@link Bars} for a given date index.
     * Useful when indicators are computed on raw Bars arrays and the result
     * needs to be looked up by universe date index.
     * Works for both equity symbols and index symbols.
     *
     * @return bar index into the symbol's Bars, or {@code -1} if no data
     */
    public int getBarIndex(String scripId, int dateIndex) {
        if (dateIndex < 0 || dateIndex >= dates.length) return -1;
        int[] map = indexMaps.get(scripId);
        if (map == null) {
            map = indexIndexMaps.get(scripId);
        }
        if (map == null) return -1;
        return map[dateIndex];
    }

    /**
     * Get the close price of an index (e.g., India VIX) at a given date index.
     * @return close price, or {@code Float.NaN} if no data
     */
    public float getIndexClose(String scripId, int dateIndex) {
        int[] map = indexIndexMaps.get(scripId);
        if (map == null || dateIndex < 0 || dateIndex >= dates.length) return Float.NaN;
        int barIdx = map[dateIndex];
        if (barIdx < 0) return Float.NaN;
        return indexBars.get(scripId).getClose(barIdx);
    }

    /**
     * Get all symbols that have valid data on the given date.
     */
    public List<String> getSymbolsOnDate(int dateIndex) {
        List<String> result = new ArrayList<>();
        for (String symbol : symbols) {
            int[] map = indexMaps.get(symbol);
            if (map != null && dateIndex >= 0 && dateIndex < map.length && map[dateIndex] >= 0) {
                result.add(symbol);
            }
        }
        return result;
    }

    /**
     * Build a {@link CrossSection} for a single date — all symbols' OHLCV at that date index.
     * Only symbols with valid data on this date are included.
     */
    public CrossSection getCrossSection(int dateIndex) {
        if (dateIndex < 0 || dateIndex >= dates.length) {
            return new CrossSection(0, new String[0],
                    new float[0], new float[0], new float[0], new float[0], new long[0]);
        }

        long ts = dates[dateIndex];
        List<String> validSymbols = new ArrayList<>();
        List<Float> opens = new ArrayList<>();
        List<Float> highs = new ArrayList<>();
        List<Float> lows = new ArrayList<>();
        List<Float> closes = new ArrayList<>();
        List<Long> volumes = new ArrayList<>();

        for (String symbol : symbols) {
            int[] map = indexMaps.get(symbol);
            if (map == null) continue;
            int barIdx = map[dateIndex];
            if (barIdx < 0) continue;

            Bars bars = barsBySymbol.get(symbol);
            validSymbols.add(symbol);
            opens.add(bars.getOpen(barIdx));
            highs.add(bars.getHigh(barIdx));
            lows.add(bars.getLow(barIdx));
            closes.add(bars.getClose(barIdx));
            volumes.add(bars.getVolume(barIdx));
        }

        int n = validSymbols.size();
        float[] oArr = new float[n];
        float[] hArr = new float[n];
        float[] lArr = new float[n];
        float[] cArr = new float[n];
        long[] vArr = new long[n];
        for (int i = 0; i < n; i++) {
            oArr[i] = opens.get(i);
            hArr[i] = highs.get(i);
            lArr[i] = lows.get(i);
            cArr[i] = closes.get(i);
            vArr[i] = volumes.get(i);
        }

        return new CrossSection(ts, validSymbols.toArray(new String[0]),
                oArr, hArr, lArr, cArr, vArr);
    }

    /**
     * Find the date index for a given timestamp using binary search.
     * @return date index, or {@code -1} if not found
     */
    public int findDateIndex(long timestamp) {
        return TimestampSearch.findExact(dates, dates.length, timestamp);
    }

    // --- Internal helpers ---

    private float getFloat(String scripId, int dateIndex, BarFloatAccessor accessor) {
        int[] map = indexMaps.get(scripId);
        if (map == null || dateIndex < 0 || dateIndex >= dates.length) return Float.NaN;
        int barIdx = map[dateIndex];
        if (barIdx < 0) return Float.NaN;
        return accessor.get(barsBySymbol.get(scripId), barIdx);
    }

    @FunctionalInterface
    private interface BarFloatAccessor {
        float get(Bars bars, int index);
    }
}

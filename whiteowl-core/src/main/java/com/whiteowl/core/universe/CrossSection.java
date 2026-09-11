package com.whiteowl.core.universe;

import lombok.Getter;

/**
 * A lightweight snapshot of all symbols' OHLCV data for a single date.
 * Produced by {@link UniverseDataFrame#getCrossSection(int)}.
 *
 * <p>All arrays are aligned: index {@code i} in every array refers to the same symbol,
 * identified by {@code symbols[i]}. Only symbols with valid data on this date are included.</p>
 */
@Getter
public final class CrossSection {

    private final long timestamp;
    private final String[] symbols;
    private final float[] opens;
    private final float[] highs;
    private final float[] lows;
    private final float[] closes;
    private final long[] volumes;

    public CrossSection(long timestamp, String[] symbols,
                        float[] opens, float[] highs, float[] lows,
                        float[] closes, long[] volumes) {
        this.timestamp = timestamp;
        this.symbols = symbols;
        this.opens = opens;
        this.highs = highs;
        this.lows = lows;
        this.closes = closes;
        this.volumes = volumes;
    }

    /**
     * Number of symbols with valid data on this date.
     */
    public int size() {
        return symbols.length;
    }

    /**
     * Compute cross-sectional percentile ranks for the given values.
     * Output ranks are in [1/N, 1.0] where N = number of non-NaN values.
     *
     * <p>Ties receive the average rank (fractional ranking).</p>
     *
     * @param values array of length {@link #size()}, one value per symbol.
     *               NaN values are excluded from ranking and receive NaN in the output.
     * @return rank-normalized values in [1/N, 1.0], same length as input
     */
    public float[] rank(float[] values) {
        int n = values.length;
        float[] ranks = new float[n];

        // Collect indices of non-NaN values
        int validCount = 0;
        int[] validIndices = new int[n];
        for (int i = 0; i < n; i++) {
            if (Float.isNaN(values[i])) {
                ranks[i] = Float.NaN;
            } else {
                validIndices[validCount++] = i;
            }
        }
        if (validCount == 0) return ranks;

        // Sort valid indices by value (ascending)
        for (int i = 0; i < validCount - 1; i++) {
            for (int j = i + 1; j < validCount; j++) {
                if (values[validIndices[j]] < values[validIndices[i]]) {
                    int tmp = validIndices[i];
                    validIndices[i] = validIndices[j];
                    validIndices[j] = tmp;
                }
            }
        }

        // Assign fractional ranks (handle ties)
        int i = 0;
        while (i < validCount) {
            int j = i;
            // Find all tied values
            while (j < validCount - 1 && values[validIndices[j + 1]] == values[validIndices[j]]) {
                j++;
            }
            // Average rank for ties: ranks are 1-based (i+1 through j+1)
            float avgRank = (i + 1 + j + 1) / 2.0f;
            for (int k = i; k <= j; k++) {
                ranks[validIndices[k]] = avgRank / validCount;
            }
            i = j + 1;
        }

        return ranks;
    }
}

package com.whiteowl.core.backtest.v2.optimization;

/**
 * An inclusive-exclusive bar index range {@code [startIndex, endIndex)} of an
 * instrument's bar series, identifying which segment of history a result was
 * computed on (development / validation / final out-of-sample).
 */
public record DataSplit(String name, int startIndex, int endIndex) {

    public static final String DEVELOPMENT = "DEVELOPMENT";
    public static final String VALIDATION = "VALIDATION";
    public static final String OUT_OF_SAMPLE = "OUT_OF_SAMPLE";

    public int size() {
        return endIndex - startIndex;
    }

    /**
     * Partition {@code totalBars} into development / validation / out-of-sample
     * ranges. The final OOS segment must never be used for optimization.
     *
     * @param totalBars total bars available
     * @param devPct    development fraction (e.g. 0.60)
     * @param valPct    validation fraction (e.g. 0.20); remainder is OOS
     * @return array of three splits, in dev → validation → OOS order
     */
    public static DataSplit[] compute(int totalBars, double devPct, double valPct) {
        if (devPct <= 0 || valPct < 0 || devPct + valPct >= 1.0) {
            throw new IllegalArgumentException(
                    "Invalid split fractions: dev=" + devPct + " val=" + valPct);
        }
        int devEnd = (int) (totalBars * devPct);
        int valEnd = devEnd + (int) (totalBars * valPct);
        return new DataSplit[]{
                new DataSplit(DEVELOPMENT, 0, devEnd),
                new DataSplit(VALIDATION, devEnd, valEnd),
                new DataSplit(OUT_OF_SAMPLE, valEnd, totalBars)
        };
    }

}

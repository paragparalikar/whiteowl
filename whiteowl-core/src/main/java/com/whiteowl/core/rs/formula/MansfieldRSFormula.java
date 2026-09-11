package com.whiteowl.core.rs.formula;

import com.whiteowl.core.rs.GroupComposite;
import com.whiteowl.core.rs.RSFormula;

/**
 * Mansfield Relative Strength (Weinstein Stage Analysis).
 *
 * Step 1: Compute raw RS ratio: RS(t) = targetClose(t) / compositeClose(t)
 * Step 2: Compute SMA of RS over `period` bars
 * Step 3: Mansfield RS = ((RS(t) / SMA_RS(t)) - 1) * 100
 *
 * Output is zero-centered:
 *   > 0 = RS is above its own SMA = outperforming trend
 *   < 0 = RS is below its own SMA = underperforming trend
 *   = 0 = RS is exactly at its moving average
 *
 * Default period: 52 for weekly, 200 for daily.
 */
public final class MansfieldRSFormula implements RSFormula {

    @Override
    public String name() {
        return "Mansfield RS";
    }

    @Override
    public float[] compute(float[] targetClose, GroupComposite composite,
                           int targetScripIndex, int period) {
        int size = composite.getSize();
        float[] compositeClose = composite.getCompositeClose();
        float[] result = new float[size];
        int effectivePeriod = Math.max(1, period);

        // Step 1: raw RS ratio
        float[] rawRS = new float[size];
        for (int t = 0; t < size; t++) {
            if (Float.isNaN(targetClose[t]) || Float.isNaN(compositeClose[t])
                    || compositeClose[t] <= 0) {
                rawRS[t] = Float.NaN;
            } else {
                rawRS[t] = targetClose[t] / compositeClose[t];
            }
        }

        // Step 2 & 3: SMA of RS, then Mansfield normalization
        for (int t = 0; t < size; t++) {
            if (t < effectivePeriod - 1 || Float.isNaN(rawRS[t])) {
                result[t] = Float.NaN;
                continue;
            }
            float sum = 0;
            int validCount = 0;
            for (int k = t - effectivePeriod + 1; k <= t; k++) {
                if (!Float.isNaN(rawRS[k])) {
                    sum += rawRS[k];
                    validCount++;
                }
            }
            if (validCount == 0) {
                result[t] = Float.NaN;
                continue;
            }
            float sma = sum / validCount;
            result[t] = ((rawRS[t] / sma) - 1f) * 100f;
        }
        return result;
    }
}

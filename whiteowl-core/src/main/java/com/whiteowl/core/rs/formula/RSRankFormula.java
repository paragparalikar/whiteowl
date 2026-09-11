package com.whiteowl.core.rs.formula;

import com.whiteowl.core.rs.GroupComposite;
import com.whiteowl.core.rs.RSFormula;

/**
 * RS Rank: percentile rank of the target scrip's rolling return among all
 * scrips in the group.
 *
 * For each bar t:
 *   1. Compute each scrip's return over the last `period` bars:
 *      return_s = (rebasedClose_s(t) / rebasedClose_s(t - period)) - 1
 *   2. Compute the target scrip's return the same way.
 *   3. Count how many scrips have a return lower than the target's.
 *   4. Percentile = count / total * 100
 *
 * Output range: 0-100.
 *   100 = strongest in the group over the lookback
 *   50  = median
 *   0   = weakest
 *
 * Default period: 63 (one quarter of daily bars).
 */
public final class RSRankFormula implements RSFormula {

    @Override
    public String name() {
        return "RS Rank";
    }

    @Override
    public float[] compute(float[] targetClose, GroupComposite composite,
                           int targetScripIndex, int period) {
        int size = composite.getSize();
        float[][] perScrip = composite.getPerScripRebased();
        int groupSize = composite.getScripCount();
        float[] result = new float[size];
        int effectivePeriod = Math.max(1, period);

        for (int t = 0; t < size; t++) {
            if (t < effectivePeriod || Float.isNaN(targetClose[t])
                    || Float.isNaN(targetClose[t - effectivePeriod])
                    || targetClose[t - effectivePeriod] <= 0) {
                result[t] = Float.NaN;
                continue;
            }

            float targetReturn = targetClose[t] / targetClose[t - effectivePeriod] - 1f;

            int below = 0;
            int total = 0;
            for (int s = 0; s < groupSize; s++) {
                if (s == targetScripIndex) continue;
                float sClose = perScrip[s][t];
                float sPrev = perScrip[s][t - effectivePeriod];
                if (Float.isNaN(sClose) || Float.isNaN(sPrev) || sPrev <= 0) continue;
                float sReturn = sClose / sPrev - 1f;
                total++;
                if (sReturn < targetReturn) below++;
            }
            if (total == 0) {
                result[t] = Float.NaN;
            } else {
                result[t] = (float) below / total * 100f;
            }
        }
        return result;
    }
}

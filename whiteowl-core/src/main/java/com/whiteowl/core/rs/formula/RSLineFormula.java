package com.whiteowl.core.rs.formula;

import com.whiteowl.core.rs.GroupComposite;
import com.whiteowl.core.rs.RSFormula;

/**
 * RS Line: raw price ratio of target scrip vs group composite.
 * Output: RS(t) = (targetClose(t) / compositeClose(t)) * 100
 *
 * Since both target and composite are rebased to 100 at their first bar,
 * this starts at ~100 and diverges based on relative performance.
 * A rising line means the target is outperforming the group.
 *
 * Period parameter is ignored.
 */
public final class RSLineFormula implements RSFormula {

    @Override
    public String name() {
        return "RS Line";
    }

    @Override
    public float[] compute(float[] targetClose, GroupComposite composite,
                           int targetScripIndex, int period) {
        int size = composite.getSize();
        float[] compositeClose = composite.getCompositeClose();
        float[] rs = new float[size];

        for (int t = 0; t < size; t++) {
            if (Float.isNaN(targetClose[t]) || Float.isNaN(compositeClose[t])
                    || compositeClose[t] <= 0) {
                rs[t] = Float.NaN;
            } else {
                rs[t] = (targetClose[t] / compositeClose[t]) * 100f;
            }
        }
        return rs;
    }
}

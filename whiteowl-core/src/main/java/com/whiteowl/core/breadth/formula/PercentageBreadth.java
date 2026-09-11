package com.whiteowl.core.breadth.formula;

import com.whiteowl.core.breadth.BreadthBar;
import com.whiteowl.core.breadth.BreadthFormula;

public final class PercentageBreadth implements BreadthFormula {

    @Override
    public String name() {
        return "Percentage";
    }

    @Override
    public BreadthBar compute(float[] closes, float[] prevCloses, long[] volumes, int scripCount) {
        float upSum = 0;
        float downSum = 0;
        for (int i = 0; i < scripCount; i++) {
            if (prevCloses[i] <= 0) continue;
            float pctChange = (closes[i] - prevCloses[i]) / prevCloses[i] * 100f;
            if (pctChange > 0) upSum += pctChange;
            else downSum += Math.abs(pctChange);
        }
        return new BreadthBar(upSum, downSum, upSum - downSum);
    }
}

package com.whiteowl.core.breadth.formula;

import com.whiteowl.core.breadth.BreadthBar;
import com.whiteowl.core.breadth.BreadthFormula;

public final class AdvanceDeclineBreadth implements BreadthFormula {

    @Override
    public String name() {
        return "Advance/Decline";
    }

    @Override
    public BreadthBar compute(float[] closes, float[] prevCloses, long[] volumes, int scripCount) {
        int advances = 0;
        int declines = 0;
        for (int i = 0; i < scripCount; i++) {
            if (closes[i] > prevCloses[i]) advances++;
            else if (closes[i] < prevCloses[i]) declines++;
        }
        return new BreadthBar(advances, declines, advances - declines);
    }
}

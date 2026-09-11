package com.whiteowl.core.breadth.formula;

import com.whiteowl.core.breadth.BreadthBar;
import com.whiteowl.core.breadth.BreadthFormula;

public final class MoneyFlowBreadth implements BreadthFormula {

    @Override
    public String name() {
        return "Money Flow";
    }

    @Override
    public BreadthBar compute(float[] closes, float[] prevCloses, long[] volumes, int scripCount) {
        float inflow = 0;
        float outflow = 0;
        for (int i = 0; i < scripCount; i++) {
            float delta = closes[i] - prevCloses[i];
            float flow = delta * volumes[i];
            if (flow > 0) inflow += flow;
            else outflow += Math.abs(flow);
        }
        return new BreadthBar(inflow, outflow, inflow - outflow);
    }
}

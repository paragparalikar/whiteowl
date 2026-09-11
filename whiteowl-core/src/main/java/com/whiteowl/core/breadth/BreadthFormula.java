package com.whiteowl.core.breadth;

/**
 * Strategy interface for computing breadth across multiple scrips at a single bar.
 */
public interface BreadthFormula {

    /**
     * @return Human-readable name of this formula (e.g., "Advance/Decline")
     */
    String name();

    /**
     * Compute breadth for a single bar across all scrips in the group.
     *
     * @param closes      close prices at current bar for each scrip (length = scripCount)
     * @param prevCloses  close prices at previous bar for each scrip (length = scripCount)
     * @param volumes     volumes at current bar for each scrip (length = scripCount)
     * @param scripCount  number of scrips with valid data at this bar
     * @return BreadthBar with positive, negative, and net values
     */
    BreadthBar compute(float[] closes, float[] prevCloses, long[] volumes, int scripCount);
}

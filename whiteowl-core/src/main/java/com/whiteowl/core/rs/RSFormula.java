package com.whiteowl.core.rs;

/**
 * Strategy interface for computing relative strength of a target scrip against a group composite.
 */
public interface RSFormula {

    /**
     * @return Human-readable name (e.g., "RS Line", "Mansfield RS")
     */
    String name();

    /**
     * Compute RS values for the target scrip against the group composite.
     *
     * @param targetClose       rebased close prices of the target scrip, aligned to timeline
     * @param composite         the group composite (rebased average and per-scrip data)
     * @param targetScripIndex  index of target scrip in composite.perScripRebased (-1 if not in group)
     * @param period            lookback period (interpretation is formula-specific)
     * @return float array of RS values, same length as composite.timestamps
     */
    float[] compute(float[] targetClose, GroupComposite composite, int targetScripIndex, int period);
}

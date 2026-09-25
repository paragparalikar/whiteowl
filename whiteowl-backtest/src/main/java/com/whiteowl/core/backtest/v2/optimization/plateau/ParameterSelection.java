package com.whiteowl.core.backtest.v2.optimization.plateau;

/**
 * The explainable outcome for one parameter: what was chosen, from which
 * region, and why.
 *
 * @param parameter       parameter name
 * @param selectedValue   value present in the finally selected combination
 * @param regionLower     plateau lower bound (NaN if no plateau)
 * @param regionUpper     plateau upper bound (NaN if no plateau)
 * @param robustnessScore selected region score (NaN if no plateau)
 * @param stable          whether a qualifying plateau existed
 * @param reason          selection explanation for the audit trail
 */
public record ParameterSelection(
        String parameter,
        double selectedValue,
        double regionLower,
        double regionUpper,
        double robustnessScore,
        boolean stable,
        String reason) {
}

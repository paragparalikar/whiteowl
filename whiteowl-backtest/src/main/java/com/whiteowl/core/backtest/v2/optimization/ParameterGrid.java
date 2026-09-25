package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.backtest.v2.model.StrategyInput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the complete, deterministic parameter grid from declared
 * {@link StrategyInput}s ({@code name, default, min, max, step}).
 *
 * <ul>
 *   <li>An input without min/max is held fixed at its default.</li>
 *   <li>An input with min/max enumerates {@code min, min+step, ... <= max}.</li>
 *   <li>Integral bounds produce {@link Integer} values; otherwise
 *       {@link Double} values are produced.</li>
 *   <li>Combinations violating any {@link ParameterConstraint} are rejected.</li>
 *   <li>If the post-constraint grid would exceed {@code maxCombinations} the
 *       generation fails fast — the caller must narrow the space or raise the
 *       cap explicitly.</li>
 * </ul>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParameterGrid {

    public static List<ParameterCombination> generate(List<StrategyInput> inputs,
                                                       List<ParameterConstraint> constraints,
                                                       int maxCombinations) {
        List<List<Number>> axes = new ArrayList<>(inputs.size());
        for (StrategyInput input : inputs) {
            axes.add(valuesOf(input));
        }
        long estimated = 1;
        for (List<Number> axis : axes) {
            estimated *= Math.max(1, axis.size());
            if (estimated > maxCombinations) {
                throw new IllegalArgumentException("Parameter grid would exceed "
                        + maxCombinations + " combinations (estimated " + estimated
                        + "). Narrow the ranges or raise maxParameterCombinations.");
            }
        }

        List<ParameterCombination> out = new ArrayList<>((int) estimated);
        int[] idx = new int[axes.size()];
        while (true) {
            ParameterCombination.Builder combo = ParameterCombination.builder();
            for (int i = 0; i < axes.size(); i++) {
                combo.put(inputs.get(i).getName(), axes.get(i).get(idx[i]));
            }
            ParameterCombination c = combo.build();
            if (satisfiesAll(c, constraints)) {
                out.add(c);
            }
            int pos = axes.size() - 1;
            while (pos >= 0 && ++idx[pos] >= axes.get(pos).size()) {
                idx[pos] = 0;
                pos--;
            }
            if (pos < 0) break;
            if (out.size() > maxCombinations) {
                throw new IllegalArgumentException("Parameter grid exceeded "
                        + maxCombinations + " valid combinations");
            }
        }
        log.info("Generated {} parameter combinations from {} inputs",
                out.size(), inputs.size());
        return out;
    }

    private static boolean satisfiesAll(ParameterCombination c, List<ParameterConstraint> constraints) {
        for (ParameterConstraint constraint : constraints) {
            if (!constraint.isValid(c.values())) {
                return false;
            }
        }
        return true;
    }

    private static List<Number> valuesOf(StrategyInput input) {
        if (!input.hasMin() || !input.hasMax()) {
            return List.of(input.getDefaultValue());
        }
        double min = input.getMinValue().doubleValue();
        double max = input.getMaxValue().doubleValue();
        double step = input.getStep() != null ? input.getStep().doubleValue() : 1.0;
        if (step <= 0) {
            throw new IllegalArgumentException("Non-positive step for input " + input.getName());
        }
        boolean integral = isIntegral(input.getDefaultValue()) && isIntegral(input.getMinValue())
                && isIntegral(input.getMaxValue()) && isIntegral(input.getStep());
        List<Number> values = new ArrayList<>();
        int count = (int) Math.floor((max - min) / step + 1e-9) + 1;
        for (int i = 0; i < count; i++) {
            double v = min + i * step;
            if (v > max) break;
            values.add(integral ? (int) Math.round(v) : v);
        }
        return values;
    }

    private static boolean isIntegral(Number n) {
        if (n == null) return true;
        return n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte
                || (n.doubleValue() == Math.rint(n.doubleValue()));
    }

}

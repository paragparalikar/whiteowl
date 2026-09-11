package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.rs.RSComputer;
import com.whiteowl.core.rs.RSFormula;
import com.whiteowl.core.rs.RSFormulaRegistry;
import com.whiteowl.core.rs.RSFormulaType;
import com.whiteowl.core.rs.RSResult;
import com.whiteowl.workbench.group.model.Group;
import com.whiteowl.workbench.group.repository.GroupRepository;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class RSIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "RS";
    private static final String SETTING_GROUP = "Group";
    private static final String SETTING_FORMULA = "Formula";
    private static final String SETTING_PERIOD = "Period";
    private static final RSFormulaType DEFAULT_FORMULA = RSFormulaType.RS_LINE;
    private static final int DEFAULT_PERIOD = 50;

    private static final Color RS_COLOR = Color.web("#a855f7");

    private final RSComputer rsComputer;
    private final GroupRepository groupRepository;
    private final RSFormulaRegistry formulaRegistry;

    public RSIndicator(RSComputer rsComputer, GroupRepository groupRepository,
                       RSFormulaRegistry formulaRegistry) {
        this.rsComputer = rsComputer;
        this.groupRepository = groupRepository;
        this.formulaRegistry = formulaRegistry;
    }

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(
                new IndicatorSetting(SETTING_GROUP, Group.class, ""),
                new IndicatorSetting(SETTING_FORMULA, RSFormulaType.class, DEFAULT_FORMULA),
                new IndicatorSetting(SETTING_PERIOD, Integer.class, DEFAULT_PERIOD)
        );
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        String groupName = resolveString(settings, SETTING_GROUP, "");
        RSFormulaType formulaType = resolveFormulaType(settings);
        int period = resolveInt(settings, SETTING_PERIOD, DEFAULT_PERIOD);

        Group group = findGroup(groupName);
        if (group == null || group.getScripIds().isEmpty()) {
            return emptyResult(bars.size(), groupName, formulaType, period);
        }

        RSFormula formula = formulaRegistry.get(formulaType.getDisplayName());
        if (formula == null) {
            return emptyResult(bars.size(), groupName, formulaType, period);
        }

        String targetScripId = bars.getScripId();
        RSResult rs = rsComputer.compute(targetScripId, groupName,
                group.getScripIds(), bars.getTimeframe(), formula, period);

        if (rs.getSize() == 0) {
            return emptyResult(bars.size(), groupName, formulaType, period);
        }

        double[] values = alignToChartBars(bars, rs.getTimestamps(), rs.getValues());
        String label = buildLabel(groupName, formulaType, period);

        double referenceLine = switch (formulaType) {
            case RS_LINE -> 100.0;
            case MANSFIELD -> 0.0;
            case RS_RANK -> 50.0;
        };

        return new SubChartResult(values, RS_COLOR, label, true, List.of(),
                null, null, null, referenceLine);
    }

    // --- Helpers ---

    private Group findGroup(String name) {
        if (name == null || name.isBlank()) return null;
        return groupRepository.loadAll().stream()
                .filter(g -> g.getName().equals(name))
                .findFirst().orElse(null);
    }

    private double[] alignToChartBars(Bars chartBars, long[] rsTimestamps, float[] rsValues) {
        int chartSize = chartBars.size();
        double[] aligned = new double[chartSize];
        int rIdx = 0;
        for (int i = 0; i < chartSize; i++) {
            long ts = chartBars.getTimestamp(i);
            while (rIdx < rsTimestamps.length && rsTimestamps[rIdx] < ts) rIdx++;
            if (rIdx < rsTimestamps.length && rsTimestamps[rIdx] == ts) {
                aligned[i] = Float.isNaN(rsValues[rIdx]) ? Double.NaN : rsValues[rIdx];
            } else {
                aligned[i] = Double.NaN;
            }
        }
        return aligned;
    }

    private SubChartResult emptyResult(int size, String groupName,
                                       RSFormulaType formulaType, int period) {
        double[] empty = new double[size];
        Arrays.fill(empty, Double.NaN);
        return new SubChartResult(empty, RS_COLOR, buildLabel(groupName, formulaType, period),
                true, List.of(), null, null, null);
    }

    private String buildLabel(String groupName, RSFormulaType formulaType, int period) {
        return INDICATOR_NAME + "(" + groupName + ", " + formulaType.getDisplayName() + ", " + period + ")";
    }

    private RSFormulaType resolveFormulaType(Map<String, Object> settings) {
        Object val = settings.get(SETTING_FORMULA);
        if (val instanceof RSFormulaType type) return type;
        if (val instanceof String s) return RSFormulaType.fromDisplayName(s);
        return DEFAULT_FORMULA;
    }

    private String resolveString(Map<String, Object> settings, String key, String defaultValue) {
        Object val = settings.get(key);
        if (val instanceof String s && !s.isBlank()) return s;
        return defaultValue;
    }

    private int resolveInt(Map<String, Object> settings, String key, int defaultValue) {
        Object val = settings.get(key);
        if (val instanceof Integer i) return i;
        if (val instanceof Number n) return n.intValue();
        return defaultValue;
    }
}

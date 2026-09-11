package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.breadth.BreadthComputer;
import com.whiteowl.core.breadth.BreadthFormula;
import com.whiteowl.core.breadth.BreadthFormulaRegistry;
import com.whiteowl.core.breadth.BreadthFormulaType;
import com.whiteowl.core.breadth.BreadthResult;
import com.whiteowl.workbench.group.model.Group;
import com.whiteowl.workbench.group.repository.GroupRepository;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class BreadthIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "Breadth";
    private static final String SETTING_GROUP = "Group";
    private static final String SETTING_FORMULA = "Formula";
    private static final String SETTING_SPAN = "Span";
    private static final BreadthFormulaType DEFAULT_FORMULA = BreadthFormulaType.ADVANCE_DECLINE;
    private static final int DEFAULT_SPAN = 1;

    private static final Color POSITIVE_COLOR = Color.web("#51cf66");
    private static final Color NEGATIVE_COLOR = Color.web("#ff6b6b");
    private static final Color NET_COLOR = Color.web("#4dabf7");

    private final BreadthComputer breadthComputer;
    private final GroupRepository groupRepository;
    private final BreadthFormulaRegistry formulaRegistry;

    public BreadthIndicator(BreadthComputer breadthComputer,
                             GroupRepository groupRepository,
                             BreadthFormulaRegistry formulaRegistry) {
        this.breadthComputer = breadthComputer;
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
                new IndicatorSetting(SETTING_FORMULA, BreadthFormulaType.class, DEFAULT_FORMULA),
                new IndicatorSetting(SETTING_SPAN, Integer.class, DEFAULT_SPAN)
        );
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        String groupName = resolveString(settings, SETTING_GROUP, "");
        BreadthFormulaType formulaType = resolveFormulaType(settings);
        int span = resolveInt(settings, SETTING_SPAN, DEFAULT_SPAN);

        Group group = findGroup(groupName);
        if (group == null || group.getScripIds().isEmpty()) {
            return emptyResult(bars.size(), groupName, formulaType.getDisplayName(), span);
        }

        BreadthFormula formula = formulaRegistry.get(formulaType.getDisplayName());
        if (formula == null) {
            return emptyResult(bars.size(), groupName, formulaType.getDisplayName(), span);
        }

        BreadthResult breadth = breadthComputer.compute(
                groupName, group.getScripIds(), bars.getTimeframe(), formula, span);

        if (breadth.getSize() == 0) {
            return emptyResult(bars.size(), groupName, formulaType.getDisplayName(), span);
        }

        // Align breadth net values to the chart's bar timestamps for histogram display
        double[] netValues = alignToChartBars(bars, breadth.getTimestamps(), breadth.getNet());

        String label = buildLabel(groupName, formulaType.getDisplayName(), span);
        // Net as histogram (positive bars green, negative bars red) with zero reference line
        return new SubChartResult(netValues, NET_COLOR, label, true, List.of(),
                netValues, POSITIVE_COLOR, NEGATIVE_COLOR, 0.0);
    }

    // --- Helpers ---

    private Group findGroup(String name) {
        if (name == null || name.isBlank()) return null;
        return groupRepository.loadAll().stream()
                .filter(g -> g.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private double[] alignToChartBars(Bars chartBars, long[] breadthTimestamps,
                                       float[] breadthValues) {
        int chartSize = chartBars.size();
        double[] aligned = new double[chartSize];
        int bIdx = 0;
        for (int i = 0; i < chartSize; i++) {
            long ts = chartBars.getTimestamp(i);
            while (bIdx < breadthTimestamps.length && breadthTimestamps[bIdx] < ts) {
                bIdx++;
            }
            if (bIdx < breadthTimestamps.length && breadthTimestamps[bIdx] == ts) {
                aligned[i] = breadthValues[bIdx];
            } else {
                aligned[i] = Double.NaN;
            }
        }
        return aligned;
    }

    private SubChartResult emptyResult(int size, String groupName, String formulaName, int span) {
        double[] empty = new double[size];
        Arrays.fill(empty, Double.NaN);
        String label = buildLabel(groupName, formulaName, span);
        return new SubChartResult(empty, NET_COLOR, label, true, List.of(),
                null, null, null);
    }

    private String buildLabel(String groupName, String formulaName, int span) {
        if (span <= 1) {
            return INDICATOR_NAME + "(" + groupName + ", " + formulaName + ")";
        }
        return INDICATOR_NAME + "(" + groupName + ", " + formulaName + ", " + span + ")";
    }

    private BreadthFormulaType resolveFormulaType(Map<String, Object> settings) {
        Object val = settings.get(SETTING_FORMULA);
        if (val instanceof BreadthFormulaType type) return type;
        if (val instanceof String s) return BreadthFormulaType.fromDisplayName(s);
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
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { /* fall through */ }
        }
        return defaultValue;
    }
}

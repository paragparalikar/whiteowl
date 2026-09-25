package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.backtest.v2.engine.StandardExitPolicy;
import com.whiteowl.core.bar.model.Timeframe;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The finalized strategy configuration — the output of a completed research
 * run. Strategy logic remains code; this is the machine-readable parameter
 * set. {@link #toJson()} produces a canonical JSON export.
 */
public record FinalConfiguration(
        String strategyId,
        List<String> scripIds,
        String direction,
        Timeframe timeframe,
        ParameterCombination parameters,
        StandardExitPolicy exitPolicy,
        float initialCapital,
        float costPercent,
        float slippagePercent,
        OptimizationMetrics inSampleMetrics,
        List<ResearchWarning> warnings) {

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"strategy\": \"").append(strategyId).append("\",\n");
        sb.append("  \"instruments\": [");
        for (int i = 0; i < scripIds.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(scripIds.get(i)).append('"');
        }
        sb.append("],\n");
        sb.append("  \"direction\": \"").append(direction).append("\",\n");
        sb.append("  \"timeframe\": \"").append(timeframe.getCode()).append("\",\n");
        sb.append("  \"parameters\": {");
        Map<String, Number> sorted = new TreeMap<>(parameters.values());
        int i = 0;
        for (Map.Entry<String, Number> e : sorted.entrySet()) {
            sb.append(i++ == 0 ? "" : ",");
            sb.append("\n    \"").append(e.getKey()).append("\": ").append(e.getValue());
        }
        sb.append("\n  },\n");
        sb.append("  \"exit\": {");
        sb.append("\n    \"initialStopAtr\": ").append(exitPolicy != null ? exitPolicy.initialStopAtr() : null).append(',');
        sb.append("\n    \"targetAtr\": ").append(exitPolicy != null ? exitPolicy.targetAtr() : null).append(',');
        sb.append("\n    \"trailingStopAtr\": ").append(exitPolicy != null ? exitPolicy.trailingStopAtr() : null).append(',');
        sb.append("\n    \"timeStopBars\": ").append(exitPolicy != null ? exitPolicy.timeStopBars() : null);
        sb.append("\n  },\n");
        sb.append("  \"costs\": {\"costPercent\": ").append(costPercent)
                .append(", \"slippagePercent\": ").append(slippagePercent).append("},\n");
        if (inSampleMetrics != null) {
            sb.append("  \"inSample\": {\"sortino\": ").append(inSampleMetrics.sortinoRatio())
                    .append(", \"cagr\": ").append(inSampleMetrics.cagr())
                    .append(", \"maxDrawdown\": ").append(inSampleMetrics.maxDrawdown())
                    .append(", \"trades\": ").append(inSampleMetrics.totalTrades())
                    .append("},\n");
        }
        sb.append("  \"warnings\": [");
        for (int j = 0; j < warnings.size(); j++) {
            if (j > 0) sb.append(", ");
            sb.append('"').append(warnings.get(j)).append('"');
        }
        sb.append("]\n}\n");
        return sb.toString();
    }

}

package com.whiteowl.core.backtest.algotest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiteowl.core.backtest.algotest.analysis.CostConfig;
import com.whiteowl.core.backtest.algotest.analysis.TradeAnalyzer;
import com.whiteowl.core.backtest.algotest.model.BacktestRequest;
import com.whiteowl.core.backtest.algotest.model.BacktestResult;
import com.whiteowl.core.backtest.algotest.model.MarginEstimate;
import com.whiteowl.core.backtest.algotest.model.ResultSummary;
import com.whiteowl.core.backtest.algotest.model.enums.Ticker;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs a parameter grid against the AlgoTest backtest api, one combination at
 * a time, and persists each run's inputs + summary metrics to CSV.
 */
@Slf4j
public class AlgoTestOptimizer {

    private static final ObjectMapper MAPPER = AlgoTestClient.mapper();

    private final AlgoTestClient client;
    private Duration pollInterval = Duration.ofSeconds(3);
    private Duration runTimeout = Duration.ofMinutes(15);
    private Duration interRunDelay = Duration.ofSeconds(1);

    // ---- post-trade analysis applied to every backtest's trade_wise_results ----
    /** Per-side slippage %, e.g. 0.5 = 0.5%. */
    private double slippagePct = 0;
    private CostConfig costConfig = CostConfig.none();
    /** Lot size — only used for PER_LOT brokerage. */
    private double lotSize = 10;
    /** DTE sets to score; empty set = all days. E.g. {∅, {0}, {0,1}}. */
    private List<Set<Integer>> dteSets = List.of(Set.of());
    /** Include trades before the ticker's expiry-regime change date. */
    private boolean includePreviousRegime = true;
    /** Fetch expiry-day margin estimate per backtest and emit %-of-margin columns. */
    private boolean marginEstimate = false;

    public AlgoTestOptimizer(AlgoTestClient client) {
        this.client = client;
    }

    public AlgoTestOptimizer slippagePct(double slippagePct) {
        this.slippagePct = slippagePct;
        return this;
    }

    public AlgoTestOptimizer costConfig(CostConfig costConfig) {
        this.costConfig = costConfig;
        return this;
    }

    public AlgoTestOptimizer lotSize(double lotSize) {
        this.lotSize = lotSize;
        return this;
    }

    public AlgoTestOptimizer dteSets(List<Set<Integer>> dteSets) {
        this.dteSets = dteSets;
        return this;
    }

    public AlgoTestOptimizer includePreviousRegime(boolean includePreviousRegime) {
        this.includePreviousRegime = includePreviousRegime;
        return this;
    }

    public AlgoTestOptimizer marginEstimate(boolean marginEstimate) {
        this.marginEstimate = marginEstimate;
        return this;
    }

    public AlgoTestOptimizer pollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
        return this;
    }

    public AlgoTestOptimizer runTimeout(Duration runTimeout) {
        this.runTimeout = runTimeout;
        return this;
    }

    /** Delay between consecutive backtest submissions to stay under rate limits. */
    public AlgoTestOptimizer interRunDelay(Duration interRunDelay) {
        this.interRunDelay = interRunDelay;
        return this;
    }

    /**
     * Runs every combination of {@code sweeps} against the {@code template}
     * request and writes all rows (inputs + summary + result_file_path) to
     * {@code csvPath}. Failures are recorded as rows with Status=ERROR rather
     * than aborting the sweep.
     *
     * <p>Restartable: when {@code csvPath} already exists, its rows are loaded
     * and a config is skipped once it has a full set of result rows (one per
     * DTE set) — identity is the flattened input columns. New rows are
     * appended under the existing header. Configs whose rows are all
     * Status=ERROR are retried.
     *
     * @return the rows written by this run (not rows from a prior run)
     */
    public List<LinkedHashMap<String, String>> optimize(BacktestRequest template,
                                                        List<ParameterSweep<?>> sweeps,
                                                        Path csvPath) {
        List<int[]> combinations = cartesian(sweeps);
        log.info("Running {} backtest combinations -> {}", combinations.size(), csvPath);

        List<LinkedHashMap<String, String>> existing = readExisting(csvPath);
        Map<Map<String, String>, Integer> doneRowCounts = new HashMap<>();
        for (LinkedHashMap<String, String> row : existing) {
            if (!"ERROR".equals(row.get("Status"))) {
                doneRowCounts.merge(inputKey(row), 1, Integer::sum);
            }
        }

        List<LinkedHashMap<String, String>> rows = new ArrayList<>(combinations.size());
        int skipped = 0;
        try (OptimizationCsvWriter.RowWriter csvWriter = existing.isEmpty()
                ? new OptimizationCsvWriter.RowWriter(csvPath)
                : OptimizationCsvWriter.RowWriter.append(csvPath,
                        List.copyOf(existing.get(0).keySet()))) {
            int i = 0;
            for (int[] combination : combinations) {
                i++;
                BacktestRequest request = deepCopy(template);
                for (int s = 0; s < sweeps.size(); s++) {
                    apply(sweeps.get(s), request.getStrategy(), combination[s]);
                }
                if (doneRowCounts.getOrDefault(ResultFlattener.inputColumns(request), 0)
                        >= dteSets.size()) {
                    skipped++;
                    log.info("[{}/{}] {} skipped — already complete in {}", i, combinations.size(),
                            describe(sweeps, combination), csvPath);
                    continue;
                }
                try {
                    BacktestResult result = client.runAndWait(request, pollInterval, runTimeout);
                    Double margin = marginEstimate
                            ? estimateMargin(request.getStrategy().getTicker(), result) : null;
                    String costDesc = describe(costConfig);
                    Map<String, ResultSummary> perDte = TradeAnalyzer.analyze(result,
                            request.getStrategy().getTicker(), slippagePct / 100.0,
                            costConfig, lotSize, dteSets, includePreviousRegime);
                    for (Map.Entry<String, ResultSummary> e : perDte.entrySet()) {
                        LinkedHashMap<String, String> row = ResultFlattener.flattenAnalyzed(
                                request, result, e.getKey(), e.getValue(), margin,
                                slippagePct, costDesc, null);
                        rows.add(row);
                        csvWriter.writeRow(row);
                    }
                    ResultSummary all = perDte.get("all");
                    log.info("[{}/{}] {} -> netProfit={} trades={} margin={}",
                            i, combinations.size(), describe(sweeps, combination),
                            all != null ? all.getOverallProfit() : null,
                            all != null ? all.getNumberOfTrades() : null, margin);
                } catch (AlgoTestException e) {
                    log.warn("[{}/{}] {} failed: {}", i, combinations.size(),
                            describe(sweeps, combination), e.getMessage());
                    LinkedHashMap<String, String> row = ResultFlattener.flattenAnalyzed(
                            request, null, "", null, null, slippagePct,
                            describe(costConfig), e.getMessage());
                    rows.add(row);
                    csvWriter.writeRow(row);
                }
                if (i < combinations.size()) {
                    sleep(interRunDelay);
                }
            }
        } catch (IOException | UncheckedIOException e) {
            throw new AlgoTestException("Failed writing results CSV to " + csvPath, e);
        }
        log.info("Wrote {} rows to {} ({} configs skipped as already complete)",
                rows.size(), csvPath, skipped);
        return rows;
    }

    /** Prior rows of {@code csvPath}, or empty when the file doesn't exist yet. */
    private static List<LinkedHashMap<String, String>> readExisting(Path csvPath) {
        if (!Files.exists(csvPath)) {
            return List.of();
        }
        try {
            List<LinkedHashMap<String, String>> rows = OptimizationCsvWriter.read(csvPath);
            log.info("Resuming from {} — {} existing rows", csvPath, rows.size());
            return rows;
        } catch (IOException e) {
            throw new AlgoTestException("Failed reading existing results CSV " + csvPath, e);
        }
    }

    /** Config identity of a CSV row: the input columns before "BacktestId". */
    private static Map<String, String> inputKey(Map<String, String> row) {
        LinkedHashMap<String, String> key = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : row.entrySet()) {
            if ("BacktestId".equals(e.getKey())) {
                break;
            }
            key.put(e.getKey(), e.getValue());
        }
        return key;
    }

    @SuppressWarnings("unchecked")
    private static <T> void apply(ParameterSweep<T> sweep,
                                  com.whiteowl.core.backtest.algotest.model.AlgoStrategy strategy,
                                  int valueIndex) {
        sweep.apply(strategy, sweep.getValues().get(valueIndex));
    }

    private static BacktestRequest deepCopy(BacktestRequest template) {
        try {
            return MAPPER.readValue(MAPPER.writeValueAsBytes(template), BacktestRequest.class);
        } catch (IOException e) {
            throw new AlgoTestException("Failed to clone request template", e);
        }
    }

    private static List<int[]> cartesian(List<ParameterSweep<?>> sweeps) {
        List<int[]> combinations = new ArrayList<>();
        combinations.add(new int[0]);
        for (ParameterSweep<?> sweep : sweeps) {
            int valueCount = sweep.getValues().size();
            List<int[]> next = new ArrayList<>(combinations.size() * valueCount);
            for (int[] prefix : combinations) {
                for (int v = 0; v < valueCount; v++) {
                    int[] combination = new int[prefix.length + 1];
                    System.arraycopy(prefix, 0, combination, 0, prefix.length);
                    combination[prefix.length] = v;
                    next.add(combination);
                }
            }
            combinations = next;
        }
        return combinations;
    }

    /** Expiry-day margin estimate built from the result's latest traded legs; null on failure. */
    private Double estimateMargin(Ticker ticker, BacktestResult result) {
        try {
            List<MarginEstimate.Position> positions =
                    TradeAnalyzer.marginPositions(result, ticker.name());
            if (positions.isEmpty()) {
                return null;
            }
            return client.estimateMargin(positions, true).margin();
        } catch (AlgoTestException e) {
            log.warn("Margin estimate failed for {}: {}", result.getId(), e.getMessage());
            return null;
        }
    }

    private static String describe(CostConfig c) {
        StringBuilder sb = new StringBuilder();
        sb.append("taxes=").append(c.taxesEnabled());
        if (c.brokerageEnabled()) {
            sb.append("+brokerage:").append(c.brokerageValue()).append('/')
                    .append(c.brokerageType() == CostConfig.BrokerageType.PER_ORDER ? "order" : "lot");
        }
        return sb.toString();
    }

    private static String describe(List<ParameterSweep<?>> sweeps, int[] combination) {
        StringBuilder sb = new StringBuilder();
        for (int s = 0; s < sweeps.size(); s++) {
            if (s > 0) {
                sb.append(' ');
            }
            sb.append(sweeps.get(s).getName()).append('=')
                    .append(sweeps.get(s).getValues().get(combination[s]));
        }
        return sb.toString();
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AlgoTestException("Interrupted between backtest runs", e);
        }
    }
}

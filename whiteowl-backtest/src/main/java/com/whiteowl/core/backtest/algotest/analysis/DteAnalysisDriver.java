package com.whiteowl.core.backtest.algotest.analysis;

import com.whiteowl.core.backtest.algotest.AlgoTestClient;
import com.whiteowl.core.backtest.algotest.AlgoTestException;
import com.whiteowl.core.backtest.algotest.AlgoTestOptimizationDriver;
import com.whiteowl.core.backtest.algotest.OptimizationCsvWriter;
import com.whiteowl.core.backtest.algotest.ResultFlattener;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Re-analysis driver for a prior optimization CSV: re-fetches each backtest's
 * trade_wise_results (no new backtests are submitted) and re-scores it per DTE
 * set with slippage + taxes + brokerage, expiry-day margin and Sortino — the
 * same columns the optimizer emits during a run.
 */
@Slf4j
public class DteAnalysisDriver {

    /** Prior optimization output to re-analyze (must contain a BacktestId column). */
    private static final Path INPUT_CSV =
            Path.of("C:\\git\\whiteowl\\algotest-results\\algotest-SENSEX-20261003-121410.csv");

    /** DTE buckets to evaluate; empty set = all days. */
    private static final List<Set<Integer>> DTE_SETS = List.of(
            Set.of(), Set.of(0), Set.of(0, 1));

    private static final double SLIPPAGE_PCT = 0.5;    // per-side slippage %
    private static final CostConfig COST =
            CostConfig.taxesPlusPerOrder(20);          // taxes + Rs20/order + 18% GST
    private static final double LOT_SIZE = 10;         // SENSEX lot size (PER_LOT only)
    private static final boolean INCLUDE_PREVIOUS_REGIME = true;
    private static final boolean MARGIN_ESTIMATE = true;

    private static final Duration FETCH_DELAY = Duration.ofMillis(250);

    public static void main(String[] args) throws Exception {
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger("com.whiteowl.core.backtest.algotest"))
                .setLevel(ch.qos.logback.classic.Level.INFO);

        List<String[]> table = readCsv(INPUT_CSV);
        String[] header = table.get(0);
        int backtestIdCol = indexOf(header, "BacktestId");
        int tickerCol = indexOf(header, "Ticker");
        int inputColCount = indexOf(header, "BacktestId");   // inputs = cols before BacktestId
        // tolerate rows from the older format where BacktestId came later — use first metrics col
        int metricStart = indexOf(header, "OverallProfit");
        inputColCount = Math.min(inputColCount, metricStart);

        AlgoTestClient client = new AlgoTestClient(AlgoTestOptimizationDriver.COOKIE.strip());
        String costDesc = "taxes=true+brokerage:" + COST.brokerageValue() + "/order";

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path output = Path.of("algotest-results", "algotest-dte-" + ts + ".csv");

        List<LinkedHashMap<String, String>> rows = new ArrayList<>();
        int i = 0;
        for (String[] fields : table.subList(1, table.size())) {
            i++;
            String backtestId = fields[backtestIdCol];
            if (backtestId == null || backtestId.isBlank()) {
                continue;
            }
            LinkedHashMap<String, String> inputs = new LinkedHashMap<>();
            for (int c = 0; c < inputColCount && c < fields.length; c++) {
                inputs.put(header[c], fields[c]);
            }
            Ticker ticker = Ticker.fromApiValue(fields[tickerCol]);
            try {
                BacktestResult result = client.getResult(backtestId);
                Double margin = MARGIN_ESTIMATE ? estimateMargin(client, ticker, result) : null;
                Map<String, ResultSummary> perDte = TradeAnalyzer.analyze(result, ticker,
                        SLIPPAGE_PCT / 100.0, COST, LOT_SIZE, DTE_SETS, INCLUDE_PREVIOUS_REGIME);
                for (Map.Entry<String, ResultSummary> e : perDte.entrySet()) {
                    rows.add(ResultFlattener.flattenAnalyzed(inputs, result, e.getKey(),
                            e.getValue(), margin, SLIPPAGE_PCT, costDesc, null));
                }
                log.info("[{}/{}] {} -> all profit={}",
                        i, table.size() - 1, backtestId,
                        perDte.get("all") != null ? perDte.get("all").getOverallProfit() : null);
            } catch (AlgoTestException e) {
                log.warn("[{}/{}] {} failed: {}", i, table.size() - 1, backtestId, e.getMessage());
                rows.add(ResultFlattener.flattenAnalyzed(inputs, null, "", null, null,
                        SLIPPAGE_PCT, costDesc, e.getMessage()));
            }
            sleep(FETCH_DELAY);
        }

        OptimizationCsvWriter.write(output, rows);
        System.out.println("Wrote " + rows.size() + " rows to " + output.toAbsolutePath());
    }

    private static Double estimateMargin(AlgoTestClient client, Ticker ticker,
                                         BacktestResult result) {
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

    private static int indexOf(String[] header, String name) {
        for (int c = 0; c < header.length; c++) {
            if (header[c].equals(name)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Column " + name + " not found in " + String.join(",", header));
    }

    /** Minimal CSV reader handling quoted cells. */
    private static List<String[]> readCsv(Path path) throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            List<String> cells = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            boolean inQuotes = false;
            for (int c = 0; c < line.length(); c++) {
                char ch = line.charAt(c);
                if (inQuotes) {
                    if (ch == '"') {
                        if (c + 1 < line.length() && line.charAt(c + 1) == '"') {
                            cur.append('"');
                            c++;
                        } else {
                            inQuotes = false;
                        }
                    } else {
                        cur.append(ch);
                    }
                } else if (ch == ',') {
                    cells.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(ch);
                }
            }
            cells.add(cur.toString());
            rows.add(cells.toArray(new String[0]));
        }
        return rows;
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedIOException(new IOException(e));
        }
    }
}

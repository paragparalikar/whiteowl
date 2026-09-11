package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.v2.dsl.TradingStrategyBase;
import com.whiteowl.core.backtest.v2.engine.BacktestEngine;
import com.whiteowl.core.backtest.v2.metrics.MetricsCalculator;
import com.whiteowl.core.backtest.v2.model.BacktestConfig;
import com.whiteowl.core.backtest.v2.model.BacktestReport;
import com.whiteowl.core.backtest.v2.model.ScripResult;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import com.whiteowl.scripting.script.ScriptCompiler;
import groovy.lang.Script;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.whiteowl.core.backtest.StrategyScriptConstants.V2_ADDITIONAL_IMPORTS;

/**
 * Standalone driver to run the ORB Groovy-script strategy against one or more scrips
 * and print the v2 backtest report (including Sortino).
 */
public class OrbScriptDriver {

    private static final float INITIAL_CAPITAL = 1_000_000f;

    public static void main(String[] args) throws Exception {
        String scriptSource = loadResourceScript("strategies/ORB_Supertrend_Nifty50.groovy");
        FileBarsRepository barsRepo = new FileBarsRepository();

        String[] scrips = { "NSE:NIFTY 50", "NSE:NIFTY BANK" };
        Timeframe tf = Timeframe.FIVE_MINUTE;

        for (String scripId : scrips) {
            System.out.println();
            System.out.println("═══════════════════════════════════════════");
            System.out.printf("  %s  (%s)%n", scripId.replace("NSE:", ""), tf.getDisplayLabel());
            System.out.println("═══════════════════════════════════════════");

            BacktestReport report = runStrategy(scriptSource, scripId, tf, barsRepo);
            if (report == null) {
                System.out.println("  No data or 0 trades.");
                continue;
            }
            System.out.println(report);
        }
    }

    private static BacktestReport runStrategy(String scriptSource, String scripId,
                                               Timeframe tf, FileBarsRepository barsRepo) throws Exception {
        Bars bars = barsRepo.load(scripId, tf);
        if (bars == null || bars.size() < 100) {
            return null;
        }

        Class<? extends Script> compiled = ScriptCompiler.compileClass(
                scriptSource, TradingStrategyBase.class, V2_ADDITIONAL_IMPORTS);
        Script instance = ScriptCompiler.instantiate(compiled);
        if (!(instance instanceof TradingStrategyBase strategy)) {
            throw new IllegalStateException("Script is not a TradingStrategyBase");
        }

        BacktestConfig config = BacktestConfig.builder()
                .timeframe(tf)
                .initialCapital(INITIAL_CAPITAL)
                .costPercent(0.01f)
                .slippagePercent(0.02f)
                .volumeParticipationPercent(100f)
                .offset(0)
                .build();

        BacktestEngine engine = new BacktestEngine(
                config.getInitialCapital(),
                config.getCostPercent(),
                config.getSlippagePercent(),
                config.getVolumeParticipationPercent(),
                null
        );
        ScripResult result = engine.run(scripId, bars.arrays(), strategy, Map.of(), Map.of());

        System.out.printf("  Bars: %d  |  Trades: %d%n", bars.size(), result.getTrades().size());

        if (result.getTrades().isEmpty()) {
            return null;
        }

        return MetricsCalculator.compute(List.of(result), config);
    }

    private static String loadResourceScript(String resourcePath) throws IOException {
        try (InputStream is = OrbScriptDriver.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) throw new IOException("Resource not found: " + resourcePath);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

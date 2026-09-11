package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.v2.dsl.TradingStrategyBase;
import com.whiteowl.core.backtest.v2.engine.BacktestEngine;
import com.whiteowl.core.backtest.v2.model.ScripResult;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import com.whiteowl.scripting.script.ScriptCompiler;
import groovy.lang.Script;

import com.whiteowl.core.indicator.Supertrend;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.whiteowl.core.backtest.StrategyScriptConstants.V2_ADDITIONAL_IMPORTS;

public final class SupertrendDebugDriver {

    private static final String SCRIPT = """
            import groovy.transform.Field

            @Field def st
            @Field def dir
            @Field int period
            @Field float mult
            @Field int qty = 50

            void setup() {
                period = input("Period", 10, 3, 50, 1) as int
                mult = input("Multiplier", 3.0, 1.0, 6.0, 0.5) as float
                println "DEBUG setup: period=" + period + " mult=" + mult
                def result = supertrend(period, mult)
                st = result[0]
                dir = result[1]
                println "DEBUG setup: st=" + st + " dir=" + dir
            }

            void onBar(int bar, String scripId) {
                if (bar < period + 2) return

                float currentDir = dir[0]
                float prevDir = dir[-1]

                if (bar < period + 10 || (bar >= 25 && bar <= 35) || (bar >= 38 && bar <= 42)) {
                    println "DEBUG bar=" + bar + " currentDir=" + currentDir + " prevDir=" + prevDir + " stVal=" + st[0] + " close=" + close[0]
                }

                if (currentDir > 0 && prevDir <= 0) {
                    if (hasOpenPositions()) shortExit()
                    longEntry(qty)
                }
                else if (currentDir < 0 && prevDir >= 0) {
                    if (hasOpenPositions()) longExit()
                    shortEntry(qty)
                }
            }
            """;

    public static void main(String[] args) throws Exception {
        FileBarsRepository repo = new FileBarsRepository();
        Bars bars = repo.loadRange("NSE:NIFTY 50", Timeframe.FIFTEEN_MINUTE, 0, 500);
        System.out.println("Loaded " + bars.size() + " bars");
        BarsArrays arrays = bars.arrays();

        System.out.println("First 5 bars:");
        for (int i = 0; i < Math.min(5, arrays.size()); i++) {
            System.out.printf("  bar %d: O=%.2f H=%.2f L=%.2f C=%.2f%n",
                    i, arrays.open()[i], arrays.high()[i], arrays.low()[i], arrays.close()[i]);
        }

        Class<? extends Script> clazz = ScriptCompiler.compileClass(SCRIPT, TradingStrategyBase.class, V2_ADDITIONAL_IMPORTS);
        Script instance = ScriptCompiler.instantiate(clazz);

        if (!(instance instanceof TradingStrategyBase strategy)) {
            System.out.println("ERROR: not a TradingStrategyBase!");
            return;
        }

        // Check @Field fields via reflection
        System.out.println("Script class: " + instance.getClass().getName());
        for (java.lang.reflect.Field f : instance.getClass().getDeclaredFields()) {
            System.out.println("  Field: " + f.getName() + " type=" + f.getType().getSimpleName());
        }

        Map<String, Number> inputs = new LinkedHashMap<>();
        inputs.put("Period", 10);
        inputs.put("Multiplier", 3.0f);
        strategy.setInputOverrides(inputs);

        // Run core Supertrend.compute for comparison
        System.out.println("\n=== CORE Supertrend.compute() comparison ===");
        float[][] coreResult = Supertrend.compute(arrays.high(), arrays.low(), arrays.close(), arrays.size(), 10, 3.0f);
        float[] coreValues = coreResult[0];
        float[] coreDirection = coreResult[1];

        int coreFlips = 0;
        for (int i = 11; i < arrays.size(); i++) {
            if (!Float.isNaN(coreDirection[i]) && !Float.isNaN(coreDirection[i - 1]) && coreDirection[i] != coreDirection[i - 1]) {
                coreFlips++;
                if (coreFlips <= 5) {
                    System.out.printf("  CORE FLIP at bar %d: dir %.0f -> %.0f, close=%.2f, st=%.2f%n",
                            i, coreDirection[i - 1], coreDirection[i], arrays.close()[i], coreValues[i]);
                }
            }
        }
        System.out.println("  Total core direction flips: " + coreFlips);

        // Print core values around the first valid bar
        System.out.println("\n  Core values bars 9-15:");
        for (int i = 9; i < Math.min(16, arrays.size()); i++) {
            System.out.printf("  bar %d: value=%.2f direction=%.0f close=%.2f%n",
                    i, coreValues[i], coreDirection[i], arrays.close()[i]);
        }

        BacktestEngine engine = new BacktestEngine(1_000_000f, 0.03f, 0.02f, 100f, null);
        ScripResult result = engine.run("NSE:NIFTY 50", arrays, strategy);
        System.out.println("\nTrades: " + result.getTrades().size());
        System.out.println("Net PnL: " + result.getTotalNetPnl());
    }
}

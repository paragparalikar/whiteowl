package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.backtest.v2.dsl.TradingStrategyBase;
import com.whiteowl.core.backtest.v2.model.StrategyInput;
import com.whiteowl.core.backtest.v2.smartvalue.FloatSmartValue;
import com.whiteowl.core.backtest.v2.smartvalue.LongSmartValue;
import com.whiteowl.scripting.script.ScriptCompiler;
import groovy.lang.Script;

import java.util.List;
import java.util.function.Supplier;

import static com.whiteowl.core.backtest.StrategyScriptConstants.V2_ADDITIONAL_IMPORTS;

/**
 * How a strategy is supplied to the optimization engine. Strategies are code —
 * there is no JSON strategy definition:
 *
 * <ul>
 *   <li>{@link #script(String)} — a Groovy DSL source string compiled once and
 *       instantiated per backtest run.</li>
 *   <li>{@link #java(Supplier)} — a hardcoded {@link TradingStrategyBase}
 *       subclass; the supplier must return a fresh instance per call because
 *       strategies carry per-run state.</li>
 * </ul>
 */
public abstract class StrategySpec {

    /** Create a fresh, unconfigured strategy instance. */
    public abstract TradingStrategyBase newInstance();

    /**
     * Declared optimizable inputs of the strategy. If not provided explicitly,
     * they are discovered by running {@code setup()} in discovery mode against
     * empty bound bar series, which records every {@code input(...)} call.
     */
    public abstract List<StrategyInput> declaredInputs();

    /** Stable identifier for audit output (class name or source hash). */
    public abstract String strategyId();

    /** Human-readable display name for reports. Defaults to {@link #strategyId()}. */
    public String name() {
        return strategyId();
    }

    public static StrategySpec script(String source) {
        return new ScriptSpec(source, null);
    }

    public static StrategySpec script(String source, String name) {
        return new ScriptSpec(source, name);
    }

    public static StrategySpec of(Supplier<TradingStrategyBase> factory) {
        return new JavaSpec(factory, null, null);
    }

    public static StrategySpec of(Supplier<TradingStrategyBase> factory, List<StrategyInput> inputs) {
        return new JavaSpec(factory, inputs, null);
    }

    public static StrategySpec of(Supplier<TradingStrategyBase> factory, String name) {
        return new JavaSpec(factory, null, name);
    }

    /**
     * Discovers inputs by instantiating the strategy, binding empty bar series,
     * and running {@code setup()} in discovery mode. Works for both Groovy
     * scripts and hardcoded Java strategies.
     */
    static List<StrategyInput> discover(Supplier<TradingStrategyBase> factory) {
        TradingStrategyBase strategy = factory.get();
        strategy.setDiscoveryMode(true);
        strategy.clearInputs();
        int maxBars = Math.max(1, strategy.getMaxBars());
        strategy.bindBarData(new FloatSmartValue(maxBars), new FloatSmartValue(maxBars),
                new FloatSmartValue(maxBars), new FloatSmartValue(maxBars),
                new LongSmartValue(maxBars), new LongSmartValue(maxBars));
        try {
            strategy.invokeSetup();
        } catch (Exception ignored) {
            // setup() may touch engine/breadth state that is absent in discovery;
            // inputs declared before the failure are still captured.
        }
        return List.copyOf(strategy.getDeclaredInputs());
    }

    // ── Implementations ─────────────────────────────────────────────────

    private static final class ScriptSpec extends StrategySpec {
        private final String source;
        private final String name;
        private volatile Class<? extends Script> compiled;

        ScriptSpec(String source, String name) {
            this.source = source;
            this.name = name;
        }

        @Override
        public TradingStrategyBase newInstance() {
            Class<? extends Script> clazz = compiled;
            if (clazz == null) {
                synchronized (this) {
                    if (compiled == null) {
                        try {
                            compiled = ScriptCompiler.compileClass(
                                    source, TradingStrategyBase.class, V2_ADDITIONAL_IMPORTS);
                        } catch (Exception e) {
                            throw new IllegalStateException("Strategy compilation failed", e);
                        }
                    }
                    clazz = compiled;
                }
            }
            Script instance = ScriptCompiler.instantiate(clazz);
            return (TradingStrategyBase) instance;
        }

        @Override
        public List<StrategyInput> declaredInputs() {
            return discover(this::newInstance);
        }

        @Override
        public String strategyId() {
            return "script#" + Integer.toHexString(source.hashCode());
        }

        @Override
        public String name() {
            return name != null ? name : strategyId();
        }
    }

    private static final class JavaSpec extends StrategySpec {
        private final Supplier<TradingStrategyBase> factory;
        private final List<StrategyInput> inputs;
        private final String name;

        JavaSpec(Supplier<TradingStrategyBase> factory, List<StrategyInput> inputs,
                 String name) {
            this.factory = factory;
            this.inputs = inputs;
            this.name = name;
        }

        @Override
        public TradingStrategyBase newInstance() {
            return factory.get();
        }

        @Override
        public List<StrategyInput> declaredInputs() {
            return inputs != null ? inputs : discover(factory);
        }

        @Override
        public String strategyId() {
            return newInstance().getClass().getName();
        }

        @Override
        public String name() {
            return name != null ? name : newInstance().getClass().getSimpleName();
        }
    }

}

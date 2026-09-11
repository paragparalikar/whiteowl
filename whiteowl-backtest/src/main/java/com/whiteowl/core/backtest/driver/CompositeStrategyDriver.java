package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.report.CompositeHtmlReport;
import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;

/**
 * Driver for the composite ORB strategy that combines the three best
 * single-side scenarios (Long-Aligned, Short-Aligned, Short-Opposite)
 * into a unified portfolio with shared capital and OR-breadth allocation.
 *
 * <h3>Steps</h3>
 * <ol>
 *   <li>Build a {@link CompositeOrbStrategy} with the 3 optimized sub-strategies.</li>
 *   <li>Evaluate all 7 ranker combinations (Gap, RS, RVOL, Gap+RS, Gap+RVOL,
 *       RS+RVOL, Gap+RS+RVOL) by running the composite backtest with each.</li>
 *   <li>Select the ranker with the best Sortino ratio.</li>
 *   <li>Print detailed results for the winning ranker.</li>
 * </ol>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mvn compile exec:java -pl whiteowl-backtest \
 *       -Dexec.mainClass=com.whiteowl.core.backtest.driver.CompositeStrategyDriver
 * </pre>
 */
public final class CompositeStrategyDriver {

    private static final Logger log = LoggerFactory.getLogger(CompositeStrategyDriver.class);

    private static final double SLIPPAGE = 0.001;
    private static final double INITIAL_CAPITAL = 1_000_000;

    // ── Sub-strategy configs (from SingleSideOptimizationDriver results) ─

    /**
     * Long-Aligned: gap-up, long on OR-high break.
     * Sharpe 1.94, Sortino 2.80, 1128 trades
     */
    private static RotationalBacktestConfig longAlignedConfig() {
        return RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe")
                .openingRangeMinutes(5)
                .barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .entryCutoffTime(LocalTime.of(14, 30))
                .exitTime(LocalTime.of(15, 15))
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(0.65)
                .trailingStopEnabled(true)
                .trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR)
                .trailingStopMultiplier(1.25)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(4.25)
                .slippage(SLIPPAGE)
                .initialCapital(INITIAL_CAPITAL)
                .maxReEntries(0)
                .atrScaling(false)
                .rankerType(RotationalBacktestConfig.RankerType.ALPHABETICAL)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.ALIGNED)
                .side(RotationalBacktestConfig.Side.LONG)
                .minGapAtr(0.30)
                .maxGapAtr(1.30)
                .minOrbRvol(0.50)
                .maxOrbIbs(0.90)
                .picks(6)
                .build();
    }

    /**
     * Short-Aligned: gap-down, short on OR-low break.
     * Sharpe 1.65, Sortino 1.48, 268 trades
     */
    private static RotationalBacktestConfig shortAlignedConfig() {
        return RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe")
                .openingRangeMinutes(5)
                .barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .entryCutoffTime(LocalTime.of(11, 0))
                .exitTime(null)  // market close
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(2.00)
                .trailingStopEnabled(true)
                .trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR)
                .trailingStopMultiplier(1.00)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(3.50)
                .slippage(SLIPPAGE)
                .initialCapital(INITIAL_CAPITAL)
                .maxReEntries(0)
                .atrScaling(false)
                .rankerType(RotationalBacktestConfig.RankerType.ALPHABETICAL)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.ALIGNED)
                .side(RotationalBacktestConfig.Side.SHORT)
                .minGapAtr(-1.55)
                .maxGapAtr(-0.45)
                .minOrbRvol(0.50)
                .maxOrbIbs(0.60)
                .picks(2)
                .build();
    }

    /**
     * Short-Opposite: gap-up, short on OR-low break (mean-reversion).
     * Sharpe 2.72, Sortino 3.97, 1056 trades — star performer.
     */
    private static RotationalBacktestConfig shortOppositeConfig() {
        return RotationalBacktestConfig.builder()
                .universeGroupName("ORB Universe")
                .openingRangeMinutes(5)
                .barMinutes(5)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .entryCutoffTime(LocalTime.of(14, 30))
                .exitTime(LocalTime.of(15, 20))
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(0.80)
                .trailingStopEnabled(false)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(4.75)
                .slippage(SLIPPAGE)
                .initialCapital(INITIAL_CAPITAL)
                .maxReEntries(0)
                .atrScaling(false)
                .rankerType(RotationalBacktestConfig.RankerType.ALPHABETICAL)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.OPPOSITE)
                .side(RotationalBacktestConfig.Side.SHORT)
                .minGapAtr(0.48)
                .maxGapAtr(1.40)
                .minOrbRvol(0.50)
                .maxOrbIbs(0.90)
                .picks(9)
                .build();
    }

    // ── Ranker variants ─────────────────────────────────────────────────

    private record RankerVariant(String label, boolean useGap, boolean useRvol, boolean useRs) {}

    private static final List<RankerVariant> RANKER_VARIANTS = List.of(
            new RankerVariant("Gap",            true,  false, false),
            new RankerVariant("RS",             false, false, true),
            new RankerVariant("RVOL",           false, true,  false),
            new RankerVariant("Gap+RS",         true,  false, true),
            new RankerVariant("Gap+RVOL",       true,  true,  false),
            new RankerVariant("RS+RVOL",        false, true,  true),
            new RankerVariant("Gap+RS+RVOL",    true,  true,  true)
    );

    // ══════════════════════════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Composite ORB Strategy — Ranker Evaluation & Combined Backtest");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        // ── Load data ────────────────────────────────────────────────
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips("ORB Universe");
        log.info("Universe: {} scrips", scripIds.size());

        BarsRepository barsRepo = new FileBarsRepository();
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.FIVE_MINUTE);
        Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.DAILY);
        log.info("Loaded {} intraday, {} daily scrips", intradayBars.size(), dailyBars.size());
        log.info("");

        // ── Build composite strategy ─────────────────────────────────
        // 3 sub-strategies: Long-Aligned (6), Short-Aligned (2), Short-Opposite (9)
        // Total picks per day: 6 + 2 + 9 = 17
        List<CompositeOrbStrategy.SubStrategy> subStrategies = List.of(
                new CompositeOrbStrategy.SubStrategy("Long-Aligned", longAlignedConfig()),
                new CompositeOrbStrategy.SubStrategy("Short-Aligned", shortAlignedConfig()),
                new CompositeOrbStrategy.SubStrategy("Short-Opposite", shortOppositeConfig())
        );

        int totalPicks = 17; // 6 + 2 + 9

        CompositeOrbStrategy strategy = new CompositeOrbStrategy(
                subStrategies, totalPicks,
                SLIPPAGE, INITIAL_CAPITAL);

        log.info("Composite strategy: {} sub-strategies, {} total picks",
                subStrategies.size(), totalPicks);
        for (CompositeOrbStrategy.SubStrategy sub : subStrategies) {
            log.info("  {} — side={} picks={} gap={} stop={} target={} trail={}",
                    sub.name(),
                    sub.config().getSide(), sub.config().getPicks(),
                    sub.config().getGapDirectionMode(),
                    fmt(sub.config().getStopMultiplier()),
                    fmt(sub.config().getTargetMultiplier()),
                    sub.config().isTrailingStopEnabled()
                            ? fmt(sub.config().getTrailingStopMultiplier()) + "/" + sub.config().getTrailingStopBasis()
                            : "off");
        }
        log.info("");

        // ── Phase 1: Evaluate ranker variants ────────────────────────
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Phase 1: Ranker Evaluation (select by best Sortino)");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        // Also include ALPHABETICAL as a baseline
        record RankerResult(String label, RotationalMetrics metrics,
                            RotationalBacktestResult fullResult) {}

        List<RankerResult> results = new ArrayList<>();

        // Baseline: Alphabetical (no ranker scoring)
        {
            log.info("── Evaluating: ALPHABETICAL (baseline) ──");
            CompositeBacktestEngine engine = new CompositeBacktestEngine(strategy);
            RotationalBacktestResult result = engine.run(intradayBars, dailyBars, null);
            RotationalMetrics m = result.getMetrics();
            log.info("  Sharpe={} Sortino={} CAGR={}% MaxDD={}% Trades={} WR={}% PnL={}",
                    fmt(m.getSharpe()), fmt(m.getSortino()),
                    fmt(m.getCagr() * 100), fmt(m.getMaxDrawdown() * 100),
                    m.getTotalTrades(), fmt(m.getWinRate() * 100), fmtPnl(netPnl(m)));
            results.add(new RankerResult("ALPHABETICAL", m, result));
            log.info("");
        }

        // All 7 configurable ranker variants
        for (RankerVariant rv : RANKER_VARIANTS) {
            log.info("── Evaluating: {} ──", rv.label());
            ConfigurableRanker ranker = new ConfigurableRanker(rv.useGap(), rv.useRvol(), rv.useRs());
            CompositeBacktestEngine engine = new CompositeBacktestEngine(strategy);
            RotationalBacktestResult result = engine.run(intradayBars, dailyBars, ranker);
            RotationalMetrics m = result.getMetrics();
            log.info("  Sharpe={} Sortino={} CAGR={}% MaxDD={}% Trades={} WR={}% PnL={}",
                    fmt(m.getSharpe()), fmt(m.getSortino()),
                    fmt(m.getCagr() * 100), fmt(m.getMaxDrawdown() * 100),
                    m.getTotalTrades(), fmt(m.getWinRate() * 100), fmtPnl(netPnl(m)));
            results.add(new RankerResult(rv.label(), m, result));
            log.info("");
        }

        // ── Phase 2: Select best by Sortino ──────────────────────────
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Phase 2: Ranker Comparison (sorted by Sortino)");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        results.sort((a, b) -> Double.compare(b.metrics().getSortino(), a.metrics().getSortino()));

        log.info("  %-16s  %7s  %7s  %7s  %7s  %6s  %6s  %10s",
                "Ranker", "Sharpe", "Sortn", "CAGR%", "MaxDD%", "Trades", "WR%", "Net PnL");
        log.info("  ------------------------------------------------------------------------------------------");
        for (RankerResult r : results) {
            RotationalMetrics m = r.metrics();
            log.info("  %-16s  %7s  %7s  %7s  %7s  %6d  %6s  %10s",
                    r.label(),
                    fmt(m.getSharpe()), fmt(m.getSortino()),
                    fmt(m.getCagr() * 100), fmt(m.getMaxDrawdown() * 100),
                    m.getTotalTrades(), fmt(m.getWinRate() * 100),
                    fmtPnl(netPnl(m)));
        }
        log.info("");

        RankerResult best = results.getFirst();
        log.info("  BEST RANKER: {} (Sortino = {})", best.label(), fmt(best.metrics().getSortino()));
        log.info("");

        // ── Phase 3: Allocation mode comparison ─────────────────────
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Phase 3: Capital Allocation Comparison — {} ranker", best.label());
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        // Run with both allocation modes using the winning ranker
        ConfigurableRanker bestRanker = null;
        for (RankerVariant rv : RANKER_VARIANTS) {
            if (rv.label().equals(best.label())) {
                bestRanker = new ConfigurableRanker(rv.useGap(), rv.useRvol(), rv.useRs());
                break;
            }
        }

        record AllocResult(String label, RotationalMetrics metrics, RotationalBacktestResult fullResult) {}
        List<AllocResult> allocResults = new ArrayList<>();

        CapitalAllocator[] allocators = {
                new OrBreadthAllocator(),
                new FixedProportionalAllocator()
        };
        for (CapitalAllocator allocator : allocators) {
            String modeLabel = allocator.toString();
            log.info("── {} ──", modeLabel);
            CompositeOrbStrategy allocStrategy = new CompositeOrbStrategy(
                    subStrategies, totalPicks,
                    SLIPPAGE, INITIAL_CAPITAL, allocator);
            CompositeBacktestEngine engine = new CompositeBacktestEngine(allocStrategy);
            // Need a fresh ranker for each run since init() is called inside
            ConfigurableRanker rankerForRun = bestRanker != null
                    ? new ConfigurableRanker(
                        RANKER_VARIANTS.stream().filter(rv -> rv.label().equals(best.label())).findFirst().get().useGap(),
                        RANKER_VARIANTS.stream().filter(rv -> rv.label().equals(best.label())).findFirst().get().useRvol(),
                        RANKER_VARIANTS.stream().filter(rv -> rv.label().equals(best.label())).findFirst().get().useRs())
                    : null;
            RotationalBacktestResult result = engine.run(intradayBars, dailyBars, rankerForRun);
            RotationalMetrics am = result.getMetrics();
            log.info("  Sharpe={} Sortino={} CAGR={}% MaxDD={}% Trades={} WR={}% PnL={}",
                    fmt(am.getSharpe()), fmt(am.getSortino()),
                    fmt(am.getCagr() * 100), fmt(am.getMaxDrawdown() * 100),
                    am.getTotalTrades(), fmt(am.getWinRate() * 100), fmtPnl(netPnl(am)));
            log.info("  Long PnL={} Short PnL={}", fmtPnl(am.getLongPnl()), fmtPnl(am.getShortPnl()));
            allocResults.add(new AllocResult(modeLabel, am, result));
            log.info("");
        }

        log.info("  ── Allocation Comparison Table ──");
        log.info("  {}", String.format("%-26s  %7s  %7s  %7s  %7s  %6s  %6s  %10s  %10s  %10s",
                "Allocation", "Sharpe", "Sortn", "CAGR%", "MaxDD%", "Trades", "WR%",
                "Net PnL", "Long PnL", "Short PnL"));
        log.info("  {}", "-".repeat(110));
        for (AllocResult ar : allocResults) {
            RotationalMetrics am = ar.metrics();
            log.info("  {}", String.format("%-26s  %7s  %7s  %7s  %7s  %6d  %6s  %10s  %10s  %10s",
                    ar.label(),
                    fmt(am.getSharpe()), fmt(am.getSortino()),
                    fmt(am.getCagr() * 100), fmt(am.getMaxDrawdown() * 100),
                    am.getTotalTrades(), fmt(am.getWinRate() * 100),
                    fmtPnl(netPnl(am)), fmtPnl(am.getLongPnl()), fmtPnl(am.getShortPnl())));
        }
        log.info("");

        // ── Phase 4: Final Summary ──────────────────────────────────
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  Phase 4: Final Composite Strategy Summary");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("");

        log.info("  Sub-strategies:");
        for (CompositeOrbStrategy.SubStrategy sub : subStrategies) {
            RotationalBacktestConfig cfg = sub.config();
            String gapRange = String.format("[%s,%s]",
                    cfg.getMinGapAtr() != null ? fmt(cfg.getMinGapAtr()) : "n/a",
                    cfg.getMaxGapAtr() != null ? fmt(cfg.getMaxGapAtr()) : "n/a");
            log.info("    {} — side={} stop={} target={} cutoff={} exit={} trail={} gap={} ibs<={} picks={}",
                    sub.name(), cfg.getSide(),
                    fmt(cfg.getStopMultiplier()), fmt(cfg.getTargetMultiplier()),
                    cfg.getEntryCutoffTime(),
                    cfg.getExitTime() != null ? cfg.getExitTime() : "MktClose",
                    cfg.isTrailingStopEnabled()
                            ? fmt(cfg.getTrailingStopMultiplier()) + "/" + cfg.getTrailingStopBasis()
                            : "off",
                    gapRange,
                    cfg.getMaxOrbIbs() != null ? fmt(cfg.getMaxOrbIbs()) : "none",
                    cfg.getPicks());
        }
        log.info("");
        log.info("  Ranker: {}", best.label());
        log.info("  Total picks: {} per day", totalPicks);
        log.info("");

        // Determine best allocation
        AllocResult bestAlloc = allocResults.stream()
                .max(Comparator.comparingDouble(ar -> ar.metrics().getSortino()))
                .orElse(allocResults.getFirst());
        log.info("  Best allocation by Sortino: {}", bestAlloc.label());
        RotationalMetrics m = bestAlloc.metrics();
        log.info("");
        log.info("  ── Performance ──");
        log.info("  Sharpe:      {}", fmt(m.getSharpe()));
        log.info("  Sortino:     {}", fmt(m.getSortino()));
        log.info("  CAGR:        {}%", fmt(m.getCagr() * 100));
        log.info("  Max DD:      {}%", fmt(m.getMaxDrawdown() * 100));
        log.info("  Trades:      {}", m.getTotalTrades());
        log.info("  Win Rate:    {}%", fmt(m.getWinRate() * 100));
        log.info("  Profit Fct:  {}", fmt(m.getProfitFactor()));
        log.info("  Calmar:      {}", fmt(m.getCalmar()));
        log.info("  Net PnL:     {}", fmtPnl(netPnl(m)));
        log.info("  Long PnL:    {}", fmtPnl(m.getLongPnl()));
        log.info("  Short PnL:   {}", fmtPnl(m.getShortPnl()));
        log.info("");

        // ── Generate HTML Report ─────────────────────────────────────
        Path reportPath = Path.of(System.getProperty("user.home"),
                ".whiteowl", "reports", "composite_orb_report.html");
        reportPath.getParent().toFile().mkdirs();
        CompositeHtmlReport.generate(bestAlloc.fullResult(), strategy,
                best.label(), bestAlloc.label(), reportPath);
        log.info("  HTML report: {}", reportPath.toAbsolutePath());
        log.info("");

        log.info("██████████████████████████████████████████████████████████████████████████████████████");
        log.info("  DONE");
        log.info("██████████████████████████████████████████████████████████████████████████████████████");
    }

    // ── Formatting helpers ──────────────────────────────────────────

    private static double netPnl(RotationalMetrics m) { return m.getLongPnl() + m.getShortPnl(); }

    private static String fmt(double v) { return String.format("%.2f", v); }

    private static String fmtPnl(double pnl) {
        if (Math.abs(pnl) >= 100_000) return String.format("%.2fL", pnl / 100_000);
        return String.format("%.0f", pnl);
    }
}

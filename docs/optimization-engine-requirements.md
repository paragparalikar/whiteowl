# Strategy Discovery & Optimization Engine — Phased Implementation Plan

> **Status of this document.** This is a reorganization of the original
> free-form requirements dump into a build order with explicit dependencies.
> All substantive requirements from the original are preserved; sections now
> live inside the phase that implements them.
>
> **Key changes vs. the original draft:**
>
> 1. **Strategies are defined in code, not JSON.** A strategy is a hardcoded
>    class (Java, or a Groovy `TradingStrategyBase` script — the existing v2
>    engine already executes these). There is no `StrategyDefinition` JSON
>    document. The *result* of optimization may still be exported as
>    machine-readable JSON, but the strategy logic itself is code.
> 2. The backtesting engine and the performance report already exist and are
>    reused, not rebuilt.
> 3. A single mutable **`OptimizationContext`** object is threaded through
>    every phase. Each phase reads what earlier phases produced and appends
>    its own artifacts. The context is the single source of truth for final
>    report generation.

---

## 0. Objective

Build a **Strategy Discovery & Optimization Engine** that accepts a
user-defined trading idea (expressed as code) and systematically transforms it
into a fully specified, statistically analyzed, documented trading strategy.

The engine must NOT simply maximize a backtest metric. Its primary objective
is to discover a **robust strategy configuration** that demonstrates:

- Stable performance across parameter ranges and neighboring parameter values
- Stable performance across time, market regimes, and timeframes
- Acceptable drawdown and good downside-risk-adjusted returns
- Low sensitivity to small parameter changes
- Minimal evidence of overfitting

The primary optimization metric is **Sortino Ratio**. The system must
explicitly prefer a **stable plateau/region of good performance** over an
isolated parameter combination producing an exceptionally high metric.

The engine supports **one direction per strategy**: LONG or SHORT. A single
strategy must never contain both long and short trades; definitions that try
to mix them are rejected.

This is a **long-running research workload**, not a synchronous
request/response operation. A single discovery job may run for hours or days,
evaluate millions of parameter combinations, generate millions of trades, and
execute in parallel across timeframes.

---

## 1. Cross-Cutting Design Principles

These apply to every phase.

### 1.1 Deterministic research

Given identical input data, strategy code, engine version, and configuration,
results must be reproducible. Persist strategy version, data version,
indicator version, engine version, optimization configuration, random seeds,
timestamps, parameter combinations, and intermediate results.

### 1.2 No look-ahead bias

Never use information unavailable at the time of trade execution. This applies
to indicators, entry/exit signals, ATR, MAE/MFE, market breadth, India VIX,
and all calendar/time features. Default execution model:

> Signal generated on bar N → trade executed at the earliest executable price
> on bar N+1.

The execution model (this-bar close / next-bar open / next-bar close) must be
configurable.

### 1.3 No survivorship bias

Where the instrument universe contains multiple securities, respect historical
membership.

### 1.4 Transaction costs

Support brokerage, exchange charges, STT, GST, stamp duty, SEBI charges,
slippage, and other configurable costs. Costs are applied **during**
backtesting. Report Gross, Net-before-costs, and Net-after-costs. Optimization
uses **net returns after all configured costs**.

### 1.5 Position model

One position at a time initially. Configurable: initial capital, fixed
quantity, fixed capital allocation, risk-based sizing. During optimization use
a normalized trade-level representation (R-multiples) so evaluation is
independent of absolute capital.

### 1.6 Minimum samples

Every statistical analysis enforces configurable minimum sample sizes
(defaults: 100 trades per strategy, 30 per filter bucket, 30 per time bucket,
20 per month). Below threshold: report `INSUFFICIENT SAMPLE`; do not optimize.

### 1.7 No hidden optimization

Every optimization decision must be explainable. For every selected parameter
store: candidate values, their performance, the plateau region, the selected
value, and a human-readable selection reason.

### 1.8 Failure isolation

A single failed parameter combination must not kill the job. Record the
failure, continue, and report successful/failed/skipped counts at the end.

---

## 2. The `OptimizationContext` — Shared Object Across All Phases

One object accompanies a research job from start to finish. Each phase reads
its inputs from the context and appends its outputs. The report generator
consumes only the context.

Conceptual contents (grow per phase):

```text
OptimizationContext
├── job: jobId, createdAt, engineVersion, configHash
├── strategyRef: class/script reference + version
├── instrument, direction, timeframes[]
├── researchConfig: all thresholds & settings (see Phase 0)
├── dataSplits: development / validation / out-of-sample ranges
├── dataVersion, dateRange, barCount
├── parameterSpace: declared inputs + generated combinations
├── optimizationResults: per (timeframe × parameterCombination) metrics   [P1]
├── plateaus / robustRegions / selectedParameters + selectionReasons      [P2]
├── walkForwardWindows + drift stats                                      [P3]
├── rawTradePopulation + maeMfeAnalysis                                   [P4]
├── exitSelection (stop/target/timeStop + robust region)                  [P4]
├── tradeFeatures + filterAnalysis + acceptedFilters                      [P5]
├── timeframeAnalysis + selectedTimeframe                                 [P6]
├── explorationFindings (VIX, breadth, calendar, streaks, ACF, ...)       [P7]
├── monteCarlo, costSensitivity, significance results                     [P8]
├── finalStrategyConfiguration                                            [P8]
├── warnings[]                                                            [all]
└── audit: tests run, hypotheses tested, counts                           [all]
```

The context must be serializable so it can be checkpointed (Phase 9) and so a
crashed job can resume.

---

## 3. Phase Map and Dependencies

```text
Phase 0  Foundations (context, config, strategy contract, parameter grid, splits)
   │
Phase 1  Core Optimization Engine (grid × timeframe → performance report)
   │
Phase 2  Robust Parameter Selection (plateau detection, robustness score)
   │
Phase 3  Walk-Forward Engine (rewrite; drives Phase-1/2 machinery across windows)
   │
Phase 4  Exit Optimization via MAE/MFE (raw trades → stop/target/time-stop regions)
   │
Phase 5  Filter & Feature Optimization (entry features, uni/multivariate filters)
   │
Phase 6  Timeframe Selection (TimeframeRobustnessAnalysis)
   │
Phase 7  Exploratory Trade Analysis (VIX, breadth, calendar, streaks, ACF, R, DD)
   │
Phase 8  Statistical Validation & Final Selection (Monte Carlo, cost sensitivity,
   │      multiple testing, complexity, warnings, final config + JSON export)
   ▼
Phase 9  Reporting & Persistence (HTML report, audit trail, checkpointing, caching)
```

Phases 3+ each feed their artifacts into `OptimizationContext`. Nothing in
later phases mutates decisions made earlier without recording it as a new
research iteration.

---

## Phase 0 — Foundations

**Goal:** everything the later phases assume exists.

**Depends on:** existing codebase (see "Existing assets" below).

**Deliverables:**

1. **Strategy contract.** A strategy is a hardcoded class exposing:
   - entry/exit logic (the existing `TradingStrategyBase` model: signal on
     bar N, fill on bar N+1 by default),
   - a list of **optimizable inputs** — reuse the existing `StrategyInput`
     model: `name, defaultValue, minValue, maxValue, step`,
   - declared direction (LONG or SHORT — reject mixed),
   - an optional explicit exit condition; if absent, only standardized exits
     apply (initial stop, trailing stop, target, time stop — each
     independently enableable).
2. **Parameter space generator.** Produce the full grid of
   `ParameterCombination`s, validate constraints (e.g. `shortEma < longEma`),
   reject invalid combos, and assign each combo an immutable
   `ParameterCombinationId` (e.g. `TIMEFRAME=15m,SHORT_EMA=9,LONG_EMA=42`).
   Enforce a configurable `maxParameterCombinations` cap.
3. **`ResearchConfiguration`.** Central config object — nothing important is
   hardcoded: plateau threshold, minimum trade counts, train/validation/test
   ratios, Monte Carlo simulation count, minimum filter samples, transaction
   costs, slippage, parallelism settings, time-stop candidates, bin counts.
4. **Data splits.** Partition history into Development / Validation /
   Final-out-of-sample with configurable ratios (e.g. 60/20/20). The final
   OOS segment is never touched by optimization.
5. **`OptimizationContext`** skeleton (per §2) + `ResearchJob` model
   (`CREATED → QUEUED → RUNNING → {PAUSED, CANCEL_REQUESTED, CANCELLED,
   FAILED, COMPLETED}`) with progress tracking (total/completed/failed tasks,
   current phase/timeframe/combination).

**Done when:** a hardcoded strategy class can be pointed at an instrument +
timeframe set, its parameter grid is generated and validated, and a context +
job record exists.

---

## Phase 1 — Core Optimization Engine

**Goal:** for a strategy + instrument, evaluate **every parameter combination
on every timeframe** and return a full performance report per combination —
numbers only, no trade-level payload required at this stage.

**Depends on:** Phase 0; the existing v2 backtest engine and
`MetricsCalculator`/`BacktestReport`.

**Deliverables:**

1. **Batch evaluation API:** `optimize(strategy, instrument, timeframes[],
   parameterCombinations[], config) → List<OptimizationResult>` where each
   result holds the `ParameterCombinationId`, timeframe, and a metrics object:
   total/winning/losing trades, win rate, average/largest win & loss, profit
   factor, payoff ratio, expectancy, net profit, CAGR, max drawdown, max
   drawdown duration, Sharpe, **Sortino**, Calmar, consecutive wins/losses,
   average/longest trade duration. Reuse `MetricsCalculator` — extend it only
   where metrics are missing (e.g. longest trade duration, median trade,
   R-multiples if absent).
2. **Bounded parallel execution** over combinations (configurable
   `workerCount`, `batchSize`); per-task failure isolation (§1.8);
   cancellation + progress callbacks. Multi-threaded and single-threaded runs
   must produce identical results.
3. **Indicator/value caching** keyed by `(instrument, timeframe, date range,
   indicator, parameters, data version)` so identical indicator series are
   not recomputed per combination.
4. Results appended to `context.optimizationResults`, each tagged with data
   split (development by default) for reproducibility.

**Done when:** a grid run over N timeframes × M combinations returns a
metrics table, runs in parallel deterministically, and a single-combination
failure does not abort the run.

---

## Phase 2 — Robust Parameter Selection

**Goal:** turn the Phase-1 result table into **stable parameter regions**, not
argmax rows.

**Depends on:** Phase 1.

**Deliverables:**

1. **Plateau detection algorithm.** For each parameter:
   - sort values, compute Sortino per value,
   - smooth the curve where appropriate, compute local gradients and local
     variance,
   - find contiguous regions where performance ≥ `plateauThreshold` × optimum
     (default 95% — e.g. max Sortino 1.40 ⇒ plateau candidates ≥ 1.33),
   - require minimum neighbor count, max performance variance, no isolated
     spikes, adequate trade count.
   - Extend to 2-parameter regions (plateau rectangles/heatmaps) where the
     grid supports it.
2. **Robustness score** per region — composite of performance, parameter
   stability, neighbor stability, trade count, drawdown stability, time
   stability, distribution stability. Sortino remains the primary metric; the
   score only breaks ties between similarly-performing regions. The formula
   must be documented and deterministic.
3. **Explainability records** (§1.7): for each selected parameter store
   candidates, performance, plateau bounds, chosen value, reason.
   Example reason: "12 lies within the stable plateau 9–15; neighbors produce
   similar Sortino."
4. Writes `PlateauRegion`s + selected configuration + reasons to the context.
5. **Check:** audit the codebase first — if any existing plateau/robustness
   code exists it is almost certainly tied to the rotational engine; extract
   the math if sound, otherwise build fresh.

**Done when:** given the example series `0.75→1.31, 1.00→1.35, 1.25→1.34,
1.50→1.33, 1.75→0.92`, the engine selects region `1.00–1.50` rather than the
point `1.00`.

---

## Phase 3 — Walk-Forward Optimization Engine

**Goal:** rolling-window out-of-sample validation built generically on top of
Phases 1–2.

**Depends on:** Phases 1–2.

**Disposition of existing code:** `WalkForwardOptimizationDriver` is
hardcoded to the rotational ORB strategy (fixed windows, strategy-specific
parameters) and is **not** reusable here — remove it (keep any purely generic
helpers) and write a strategy-agnostic engine.

**Deliverables:**

1. **Generic `WalkForwardEngine`** that takes an `OptimizationProcedure`
   handle (Phase 1 + Phase 2 bundled as "given this data window, return the
   robust configuration") plus window config: training length, evaluation
   length, step (all configurable; anchored vs. rolling windows supported).
2. Per-window record → `WalkForwardWindow`: training period, validation
   period, selected parameters, Sortino, CAGR, drawdown, trade count.
3. **Parameter drift analysis:** per parameter — mean, median, stddev, min,
   max, coefficient of variation across windows; flag `HIGH PARAMETER
   INSTABILITY` (e.g. 9,9,12,9,12 = stable; 3,21,6,18,12 = unstable).
4. **Walk-forward efficiency** (OOS/IS ratio) and aggregate stitched-OOS
   metrics.
5. Discovery-vs-production discipline (§Phase 8): after finalization, run one
   untouched final-OOS backtest, freeze it, never retrofit.

**Done when:** the engine runs N rolling windows for an arbitrary hardcoded
strategy, records every window, and flags parameter drift.

---

## Phase 4 — Exit Optimization via MAE/MFE

**Goal:** choose the standardized exits (initial stop, target, time stop,
optional trailing stop) from trade-level excursion analysis — not by guessing.

**Depends on:** Phases 1–2 (needs trade-level backtest access + plateau
machinery).

**Deliverables:**

1. **Raw trade generation:** for each `timeframe × entry configuration`, run
   the backtest with the **maximum configured time stop** and no other exits,
   to produce the raw trade population. Capture per trade:
   `tradeId, entryTimestamp, entryPrice, entryBarIndex, entryATR,
   exitTimestamp, exitPrice, direction, quantity, grossPnL, netPnL, MAE, MFE,
   MAE_ATR, MFE_ATR, MAE_bar, MFE_bar, maxHoldingBars`.
   This requires the backtester to expose a low-level trade API (trades,
   equity curve, MAE/MFE) without re-running unrelated analytics — extend the
   existing engine if it doesn't already.
2. **MAE/MFE definitions** (normalized by ATR at entry; LONG: `MAE = min(low)
   − entry`, `MFE = max(high) − entry`; SHORT mirrored; bar index counts from
   entry bar = 0).
3. **Exit candidate generation:** stops from the MAE distribution (e.g.
   0.5–2.0 ATR), targets from the MFE distribution, time stops from
   time-to-MFE (e.g. 5/10/15/20 bars).
4. **Exit combination evaluation:** run stop × target × time-stop combos;
   compute CAGR, total return, Sortino, Sharpe, maxDD, Calmar, profit factor,
   win rate, expectancy, trade count, average/median trade, R-multiple
   distribution; select the **robust region** via Phase 2 — never plain
   argmax.
5. Regenerate the complete trade dataset with the optimized exits; store
   trades + selection in the context.

**Done when:** for a given entry configuration the engine proposes a robust
exit region with an explainable reason, and the regenerated trade population
is stored.

---

## Phase 5 — Filter & Feature Optimization

**Goal:** test whether entry-time features improve the strategy; accept
filters only with strong evidence.

**Depends on:** Phase 4 (complete trades exist).

**Deliverables:**

1. **Feature collection at trade entry** (reuse/extend the existing
   `FeatureCollector`/`FeatureDefinition`/`TradeLifecycleCallback`
   machinery):
   - trend strength: ADX, configurable momentum,
   - trend direction: ADX, RSI (configurable lookbacks),
   - volatility: `StdDev(close)/MA(close)`, `ATR/MA(close)`,
   - overbought/oversold: RSI.
2. **Univariate filter analysis:** per feature — ordered bins; per-bin trade
   stats, Sortino, trade count, confidence intervals; identify stable
   profitable regions; reject thin samples. Accept a filter only with:
   meaningful improvement, stable improvement, sufficient sample,
   neighbor-region stability, and out-of-sample confirmation. Candidate
   forms: `ADX > X`, `RSI < X`, `Volatility > X`, etc.
3. **Staged multivariate analysis:** singles → selected pairs → selected
   triples. No exhaustive high-dimensional brute force unless explicitly
   configured. Stop when incremental improvement is insignificant/unstable.
4. **Re-optimization:** with accepted filters active, re-run robust selection
   over entry parameters and exit parameters (Phase 2 machinery) on the full
   stack (entry + filters + stops + target + time stop).
5. Apply the complexity penalty lens (Phase 8 defines the metric; record
   counts here).

**Done when:** the engine can prove (or disprove) that a filter helps, records
the evidence, and re-selects robust params for the full strategy stack.

---

## Phase 6 — Timeframe Selection

**Goal:** pick the timeframe from the most robust performance region — not the
highest single Sortino.

**Depends on:** Phases 1–5 run per timeframe.

**Deliverables:**

1. Per-timeframe summary: Sortino, Sharpe, CAGR, maxDD, Calmar, profit
   factor, win rate, expectancy, trade count.
2. **`TimeframeRobustnessAnalysis`:** Sortino stability, parameter stability,
   trade-count adequacy, drawdown stability, performance consistency over
   time, transaction-cost sensitivity. A slightly-lower-Sortino but stabler
   timeframe must win.
3. Selected timeframe + justification → context.

---

## Phase 7 — Exploratory Trade Analysis

**Goal:** characterize the finalized strategy's relationship with features.
Findings are **classified** `Descriptive / Hypothesis / PotentialFilter /
PotentialRiskControl / PotentialPositionSizingSignal` and are **never**
auto-incorporated into the strategy.

**Depends on:** Phase 6 (final strategy + full trade set).

**Deliverables — each with configurable binning and minimum-sample guards:**

1. **India VIX regime:** VIX binning → per-bin trade count, win rate,
   avg/median R, Sortino, maxDD, profit factor, expectancy; `Sortino vs VIX`
   and `Return vs VIX`; stable regions only.
2. **Market breadth:** configurable breadth series (advance/decline ratio &
   line, % above 20/50/200 DMA, new highs/lows; configurable lookbacks) →
   success probability, average return, Sortino, drawdown.
3. **Time-of-day:** entry hour/minute/minutes-from-open → configurable
   buckets → count, win rate, avg/median R, Sortino, profit factor.
4. **Calendar:** day-of-week, week-of-month, month-of-year → count, win rate,
   avg R, Sortino.
5. **Streaks & sequences:** P(next win|streak of N wins/losses), avg/median
   next R, per-condition Sortino, sample counts; WIN/LOSS transition matrix
   (P(W|W), P(L|W), P(W|L), P(L|L)) vs unconditional; significance testing.
6. **Autocorrelation:** ACF of trade returns, R-multiples, and equity-curve
   *changes* (never the raw cumulative curve) at configurable lags. If
   positive: study win/loss clustering and evaluate an equity-curve regime
   filter (`trade only when equity > its MA`) as a walk-forward-tested
   hypothesis. If negative: study post-win/post-loss sizing — **never
   auto-increase size after a loss**.
7. **R-multiple distribution:** R = netPnL / initialRisk; histogram, KDE,
   mean/median/std, skew, kurtosis, percentiles (5/25/50/75/95); descriptive
   skew classification.
8. **Drawdown analysis:** max/average/median/95th/99th percentile DD, max &
   average DD duration, equity + drawdown + underwater curves, DD by
   year/regime/VIX regime.

**Done when:** each analysis produces a classified finding record in the
context, with sample counts and significance where practical.

---

## Phase 8 — Statistical Validation & Final Selection

**Goal:** quantify overfitting risk and freeze the final configuration.

**Depends on:** Phases 3–7.

**Deliverables:**

1. **Monte Carlo** on the trade sequence (reshuffle + bootstrap): expected &
   worst CAGR, expected/95th/99th maxDD, probability of a losing year,
   probability of ruin at configured risk.
2. **Transaction-cost sensitivity:** rerun at 0.5×/1×/1.5×/2× slippage;
   measure Sortino/CAGR degradation and DD change; flag fragile strategies.
3. **Statistical significance toolkit:** bootstrap & binomial CIs,
   permutation tests, Mann-Whitney U, Welch's t, Ljung-Box — without assuming
   normal returns.
4. **Multiple-testing accounting:** track counts of configurations/filters/
   hypotheses tested; Bonferroni/Holm and FDR corrections where appropriate;
   disclose search scale in the report.
5. **Complexity penalty:** count entry conditions, filters, optimized
   parameters, special rules, regime filters, sizing rules; warn when
   complexity grows without OOS improvement.
6. **Research warnings engine** — never suppressed by a high Sortino:
   `LOW TRADE COUNT`, `HIGH PARAMETER SENSITIVITY`, `HIGH COMPLEXITY`,
   `POOR OUT-OF-SAMPLE PERFORMANCE`, `HIGH WALK-FORWARD PARAMETER DRIFT`,
   `HIGH TRANSACTION COST SENSITIVITY`, `HIGH DRAWDOWN`,
   `UNSTABLE REGIME PERFORMANCE`, `STATISTICALLY WEAK EXPLORATION RESULT`,
   `POSSIBLE OVERFITTING`, `INSUFFICIENT DATA`.
7. **Final selection + frozen OOS:** assemble instrument, direction,
   timeframe, entry (+params), filters (+thresholds), exits (stop/target/
   trailing/time-stop), sizing, cost assumptions, and the three data periods.
   Run the untouched final-OOS backtest once; freeze; never retro-fit.
8. **Machine-readable export** of the final *configuration* (JSON — this is
   the selected parameter set, not strategy logic, which remains code).

**Done when:** `StrategyDiscoveryResult` exists: final config, robust regions,
selected values + reasons, dev/validation/OOS stats, walk-forward stats,
Monte Carlo stats, exploration findings, warnings, metadata.

---

## Phase 9 — Reporting, Persistence & Operations

**Goal:** turn the context into a self-contained report and make long jobs
survivable. (Pieces of this may be pulled earlier pragmatically — e.g. basic
persistence is useful from Phase 1 — but completion is last.)

**Depends on:** all phases.

**Deliverables:**

1. **Self-contained HTML report** (all CSS/JS embedded, no external server,
   interactive charts where practical) with sections:
   - Executive summary (strategy, instrument, direction, timeframe, the three
     data periods, headline metrics, and factual statuses: `STABLE`,
     `MODERATE INSTABILITY`, `HIGH INSTABILITY`, `INSUFFICIENT DATA`),
   - Strategy definition report (entry, filters, exits, instrument,
     timeframe, session, execution assumptions, costs),
   - Equity/drawdown/underwater curves + capital/CAGR/DD numbers,
   - Timeframe comparison (Sortino bar chart with values),
   - Parameter sensitivity charts (param → Sortino with robust region
     highlighted) for entry, filter, stop, target, time-stop, trailing params,
   - VIX, breadth, calendar heatmaps (metric-switchable), streak analysis
     with CIs, ACF chart, R-multiple histogram/CDF/box, MAE/MFE
     distributions + scatter + percentile tables,
   - Walk-forward table + Sortino-across-windows + parameter drift,
   - Monte Carlo distributions, transaction-cost sensitivity,
   - **Research audit trail**: data source/version, date range, bar count,
     trade count, combinations/filters/hypotheses tested, engine version,
     timestamp, config hash.
2. **Persistence model** — entities approximately:
   `StrategyRef, ResearchJob, ResearchRun, TimeframeConfiguration,
   ParameterDefinition, ParameterCombination, RawTrade, TradeFeature,
   TradeStatistics, OptimizationResult, PlateauRegion, WalkForwardWindow,
   MonteCarloResult, ExplorationResult, StrategyConfiguration,
   ResearchReport`. Immutable IDs everywhere.
3. **Checkpoint & resume:** persist per-phase, per-timeframe,
   per-combination completion; on crash resume from last checkpoint.
4. **Caching:** OHLC, all indicators, breadth, VIX keyed as in Phase 1.
5. **Performance hygiene:** primitive arrays/columnar trade storage, batch
   persistence, no object churn in tight loops, no full-dataset copies.

---

## Critical Principle

This is not a *"find the parameters with the highest historical Sortino"*
engine. It is a *"discover statistically defensible, stable, simple,
reproducible strategies from a user-defined hypothesis"* engine. Throughout,
prioritize robustness, stability, reproducibility, out-of-sample validation,
parameter plateaus, statistical validity, low complexity, cost resilience,
and drawdown control over maximum historical return/Sortino/win rate/CAGR.
Make it hard for anyone — including the engine itself — to accidentally
manufacture an impressive-looking overfit strategy.

---

## Appendix — Existing Assets (codebase audit)

Found in `whiteowl-backtest`; reuse where noted:

| Area | Existing code | Disposition |
| ---- | ------------- | ----------- |
| Backtest engine | `backtest.v2.engine.BacktestEngine`, `TradingStrategyBase` | Reuse; extend for trade-level MAE/MFE API |
| Performance report | `backtest.v2.metrics.MetricsCalculator`, `BacktestReport` | Reuse for Phase 1; add missing metrics |
| Optimizable inputs | `backtest.v2.model.StrategyInput`, `StrategyInputDiscoverer` | Reuse as parameter-definition model |
| Feature capture | `backtest.v2.feature.*` (`FeatureCollector`, `TradeLifecycleCallback`, ...) | Reuse/extend for Phases 5 & 7 |
| Optimization | `rotational.optimizer.ParameterOptimizationEngine`, assorted `driver/*OptimizationDriver` | Strategy-specific, ≤2 params; concepts only, not reused as-is |
| Walk-forward | `driver/WalkForwardOptimizationDriver` | Rotational-ORB-specific; remove and rewrite generically (Phase 3) |
| Indicators | `backtest.v2.indicator.*` (EMA, SMA, RSI, ADX, ATR, StdDev, Supertrend, ...) | Reuse + cache |

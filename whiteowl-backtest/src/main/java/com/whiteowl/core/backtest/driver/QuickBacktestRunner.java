package com.whiteowl.core.backtest.driver;

import com.whiteowl.core.backtest.rotational.*;
import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;
import com.whiteowl.core.bar.repository.FileBarsRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Quick one-off backtest runner for ad-hoc parameter testing.
 */
public class QuickBacktestRunner {

    public static void main(String[] args) throws Exception {
        String universeName = "ORB Universe - 250";
        List<String> scripIds = RotationalOrbDriver.loadGroupScrips(universeName);

        BarsRepository barsRepo = new FileBarsRepository();
        Map<String, Bars> intradayBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.FIVE_MINUTE);
        Map<String, Bars> dailyBars = RotationalOrbDriver.loadIntradayBars(
                barsRepo, scripIds, Timeframe.DAILY);

        // Optimized config from 1-year run, with user overrides:
        // slippage = 0.2%, maxGapAtr = null (disabled)
        RotationalBacktestConfig config = RotationalBacktestConfig.builder()
                .universeGroupName(universeName)
                .startDate(LocalDate.now().minusYears(1))
                .openingRangeMinutes(5)
                .barMinutes(5)
                .side(RotationalBacktestConfig.Side.SHORT)
                .gapDirectionMode(RotationalBacktestConfig.GapDirectionMode.OPPOSITE)
                .entryMethod(RotationalBacktestConfig.EntryMethod.BREAKOUT)
                .minGapAtr(0.48)
                .maxGapAtr(null)       // disabled per user request
                .stopBasis(RotationalBacktestConfig.StopBasis.OR_RANGE)
                .stopMultiplier(1.50)
                .targetEnabled(true)
                .targetBasis(RotationalBacktestConfig.TargetBasis.STOP_DISTANCE)
                .targetMultiplier(5.75)
                .trailingStopEnabled(true)
                .trailingStopBasis(RotationalBacktestConfig.StopBasis.ATR)
                .trailingStopMultiplier(3.00)
                .entryCutoffTime(LocalTime.of(14, 0))
                .exitTime(LocalTime.of(15, 20))
                .rankerType(RotationalBacktestConfig.RankerType.RS_RVOL)
                .picks(8)
                .maxOrbIbs(0.40)
                .slippage(0.002)       // 0.2% per user request
                .initialCapital(1_000_000)
                .atrScaling(false)
                .build();

        RotationalBacktestEngine engine = new RotationalBacktestEngine(config);
        RotationalBacktestResult result = engine.run(intradayBars, dailyBars, null);
        RotationalMetrics m = result.getMetrics();

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  SHORT OPPOSITE — 0.2% slippage, no maxGapAtr filter");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.printf("  Sortino:        %.2f%n", m.getSortino());
        System.out.printf("  Sharpe:         %.2f%n", m.getSharpe());
        System.out.printf("  CAGR:           %.2f%%%n", m.getCagr() * 100);
        System.out.printf("  Max Drawdown:   %.2f%%%n", m.getMaxDrawdown() * 100);
        System.out.printf("  Profit Factor:  %.2f%n", m.getProfitFactor());
        System.out.printf("  Win Rate:       %.2f%%%n", m.getWinRate() * 100);
        System.out.printf("  Total Trades:   %d%n", m.getTotalTrades());
        System.out.printf("  Net PnL:        %.0f%n", m.getLongPnl() + m.getShortPnl());
        System.out.printf("  Calmar:         %.2f%n", m.getCalmar());
        System.out.println("═══════════════════════════════════════════════════════════");
    }
}

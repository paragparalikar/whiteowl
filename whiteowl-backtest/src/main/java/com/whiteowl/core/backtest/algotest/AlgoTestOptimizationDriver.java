package com.whiteowl.core.backtest.algotest;

import com.whiteowl.core.backtest.algotest.analysis.CostConfig;
import com.whiteowl.core.backtest.algotest.model.AlgoStrategy;
import com.whiteowl.core.backtest.algotest.model.BacktestRequest;
import com.whiteowl.core.backtest.algotest.model.LegConfig;
import com.whiteowl.core.backtest.algotest.model.enums.ExpiryType;
import com.whiteowl.core.backtest.algotest.model.enums.LegTgtSLType;
import com.whiteowl.core.backtest.algotest.model.enums.LegType;
import com.whiteowl.core.backtest.algotest.model.enums.LotType;
import com.whiteowl.core.backtest.algotest.model.enums.PositionType;
import com.whiteowl.core.backtest.algotest.model.enums.Ticker;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Optimization driver for AlgoTest backtests.
 *
 * Setup: open https://algotest.in in a browser while logged in, open devtools
 * (F12) -> Network -> any api.algotest.in request -> copy the full "Cookie"
 * request-header value and paste it into {@link #COOKIE} below. The JWT inside
 * expires after a few days — refresh it when runs start failing with 401/403.
 *
 * The strategy template below mirrors request-sample.txt (SENSEX straddle:
 * sell weekly CE+PE, 10 lots, 25pt SL, entry 09:20, exit 15:25).
 * Edit the sweeps to change what is optimized.
 */
public class AlgoTestOptimizationDriver {

    /**
     * Paste the full Cookie request-header value here. Must contain at least
     * "access_token_cookie=...; csrf_access_token=...". Example:
     * "access_token_cookie=eyJhbG...; csrf_access_token=97ec...; _ga=..."
     */
    public static final String COOKIE = """
            _fbp=fb.1.1790571712712.19216971637278357; _gcl_au=1.1.219425985.1790571713.-.-.1790959846.2137535539.1790959847.1790959846; _ga_MS6Z4BR=GS2.1.s1791371904$o8$g0$t1791371904$j60$l0$h0; _gid=GA1.2.1637136777.1791371904; _ga=GA1.1.1576540960.1790571713; _uetsid=cfcc1630c24011f1a47d9330765c3cd2; _uetvid=b8cc43b0baf911f18c7cd12bc23d7b5f; _clck=tp1oru%5E2%5Ega3%5E0%5E2462; access_token_cookie=eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJmcmVzaCI6ZmFsc2UsImlhdCI6MTc5MTM3MTkxNCwianRpIjoiZjYyNWEwMzMtYTQyOS00ZjIyLWIzNWUtYTNlZGU3YjdiYjNkIiwidHlwZSI6ImFjY2VzcyIsInN1YiI6IjZhYmEyYmY5NDAwZDZlYTQ3MzFmMzJjZiIsIm5iZiI6MTc5MTM3MTkxNCwiY3NyZiI6IjQzOTdhYzNjLTBiMzktNGRjYi05ZDAxLWIxYjdmMmJkMTY4MCIsImV4cCI6MTc5MTYzMTExNH0.YOlRRG9ZDTSAPA_Tk6umpvjBQJpXAuOEjwjeh4iwQQmS_1Urd-o-X2iY_OpXA4bjR06HFvEBzpC16ykMbsJ7SwjQgJwA1e9sqKhy4ucYAj2YY-QmJbXEkFPe3R6HdqFixnNAYAFthseM86hUGqO_UWJtYsXlLOpX-TkZBJWQQWQ24mkhCbyqzHPXgJ_G40pEfAqzLsPmU65zju7y4x5GjBlPepuA4is97b57tbX-YOn8QPgaQWEyRS-1kACEkXeJ4M-O5blUF0ClI_bsaAWYOQh9MwP7ltTMoN9Smx_17ld_uGd_P_WTlYsoOfrY2NDuWXy40MIeJh-EgET3MumHAFn_dixktVQznLr63muWTMEy6AGNTe7z8pub8N6FMOiZvrxHI1aOLvMkkh6qnsz-m3JA01F0dhVtUT6NDwFpAbiJGsqSKi7bIPKuUB_ZbM5-jeg4np304acWHfKDCK1JGyuRn6TUgU-snmS_IfH3VSY6IQImDCAErNCeafyJnFbg; csrf_access_token=4397ac3c-0b39-4dcb-9d01-b1b7f2bd1680; _clsk=1y361dc%5E1791371927406%5E3%5E1%5Eu.clarity.ms%2Fcollect; _ga_Y0EK98JRBT=GS2.1.s1791371904$o13$g1$t1791371965$j59$l0$h0
            """;

    private static final String START_DATE = "2023-03-01";
    private static final String END_DATE = "2026-10-03";

    public static void main(String[] args) {
        // project logback.xml runs root at WARN — raise our package to INFO for progress
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger("com.whiteowl.core.backtest.algotest"))
                .setLevel(ch.qos.logback.classic.Level.INFO);

        // ---------- strategy template ----------
        AlgoStrategy strategy = new AlgoStrategy();
        strategy.setTicker(Ticker.SENSEX);
        strategy.setEntryTime(9, 20);
        strategy.setExitTime(15, 14);

        LegConfig ce = new LegConfig();
        ce.setPositionType(PositionType.SELL);
        ce.setInstrumentKind(LegType.CE);
        ce.setExpiryKind(ExpiryType.WEEKLY);
        ce.setLots(LotType.QUANTITY, 10);
        ce.setClosestPremium(100);
        ce.setStopLoss(LegTgtSLType.PERCENTAGE, 35);
        strategy.addLeg(ce);

        LegConfig pe = new LegConfig();
        pe.setPositionType(PositionType.SELL);
        pe.setInstrumentKind(LegType.PE);
        pe.setExpiryKind(ExpiryType.WEEKLY);
        pe.setLots(LotType.QUANTITY, 10);
        pe.setClosestPremium(100);
        pe.setStopLoss(LegTgtSLType.PERCENTAGE, 35);
        strategy.addLeg(pe);

        BacktestRequest template = BacktestRequest.of(strategy, START_DATE, END_DATE);
        template.getAttributes().setTemplate("Straddle920");

        // ---------- optimization grid ----------
        // Closest Premium 200..400 step 25 x leg SL% 0..100 step 10 x leg Tgt% 0..100 step 10
        // (applied to every leg; use s.getListOfLegConfigs().get(i) for a specific leg)
        List<ParameterSweep<?>> sweeps = List.of(
                ParameterSweep.numericRange(SweepParameters.CLOSEST_PREMIUM, 200, 400, 25,
                        (s, v) -> s.forEachLeg(l -> l.setClosestPremium(v))),
                ParameterSweep.numericRange(SweepParameters.LEG_STOP_LOSS_PCT, 0, 100, 10,
                        (s, v) -> s.forEachLeg(l -> l.setStopLoss(LegTgtSLType.PERCENTAGE, v))),
                ParameterSweep.numericRange(SweepParameters.LEG_TARGET_PCT, 0, 100, 10,
                        (s, v) -> s.forEachLeg(l -> l.setTarget(LegTgtSLType.PERCENTAGE, v))));

        // ---------- run ----------
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path output = Path.of("algotest-results",
                "algotest-" + strategy.getTicker().name() + "-" + timestamp + ".csv");

        AlgoTestClient client = new AlgoTestClient(COOKIE.strip());
        new AlgoTestOptimizer(client)
                .slippagePct(0.5)                                   // 0.5% slippage per side
                .costConfig(CostConfig.taxesPlusPerOrder(20))       // taxes + Rs20/order + 18% GST
                .lotSize(10)                                        // SENSEX lot size
                .dteSets(List.of(Set.of(), Set.of(0), Set.of(1), Set.of(0, 1)))// all / 0DTE / 1DTE / 0+1 DTE
                .marginEstimate(true)                               // expiry-day margin per config
                .optimize(template, sweeps, output);
        System.out.println("Results written to " + output.toAbsolutePath());
    }
}

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
            _fbp=fb.1.1790571712712.19216971637278357; _gid=GA1.2.1804435019.1790952797; _gcl_au=1.1.219425985.1790571713.-.-.1790959846.2137535539.1790959847.1790959846; _ga_MS6Z4BR=GS2.1.s1790998536$o5$g0$t1790998536$j60$l0$h0; _ga=GA1.1.1576540960.1790571713; _uetsid=007ef510be7111f1b114af59a8ca2a70; _uetvid=b8cc43b0baf911f18c7cd12bc23d7b5f; _clck=tp1oru%5E2%5Eg9z%5E0%5E2462; access_token_cookie=eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJmcmVzaCI6ZmFsc2UsImlhdCI6MTc5MDk5ODU0NiwianRpIjoiZDJjYjc2NzAtODg2NS00MzA2LWFkMTctZmIxN2I5NmM4ODUzIiwidHlwZSI6ImFjY2VzcyIsInN1YiI6IjZhYmEyYmY5NDAwZDZlYTQ3MzFmMzJjZiIsIm5iZiI6MTc5MDk5ODU0NiwiY3NyZiI6Ijk3ZWMyNTdlLTVhMjItNDZlMS05NDhlLWNlY2JhN2FhZjU4NyIsImV4cCI6MTc5MTI1Nzc0Nn0.CfYTHwRFT4hS-V5juYepvZi5cWJopCFeRXtCAT5Pd3xxadmbLRs1l30GDHwNh22wQOnVXe6TxLM0dfpVC6n_Cej09w1v5XrThVarvhl19Osl8xMIvE2OFHdVCFQue8ulVN-Z6Tts8a6_dTcGvMt3OgalUbiJjuD_pcgii63OK5Ni8zRCNdiREIo38FX0iFdR4eXs61lwNwm47DzJNjv3rRIGYmH2ZhijcDWY9hhxz1C2WkCz5D4cmMNSSLUI_wrSEy1kg4g84Mdtr8UUfLTJcL5uMFKrJFOw0m_cPDQVe2QfK7flbk5KmyO3ZzDtP6bK-Fd0kaGWlcXJ3aM2uPs7am1TFANnmjN-wHNOGtMlsXE6GJBE3S4NkzD3D5Lvt7f620qmcaFby6_DxmOBGSFPknDk18g4O7Z9CQvXfDp_6k-z92rDnjt7j9rdRQRMJmUYY5xEQPwLPPtXsKLKonxJZohghfkJdrfzJGZ49O0vuhJbZfmWD10cUHzRDGQaAogo; csrf_access_token=97ec257e-5a22-46e1-948e-cecba7aaf587; _ga_Y0EK98JRBT=GS2.1.s1791002974$o5$g1$t1791003018$j16$l0$h0; _clsk=ik0kkx%5E1791003746825%5E5%5E1%5Ee.clarity.ms%2Fcollect
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
        strategy.setTicker(Ticker.NIFTY);
        strategy.setEntryTime(9, 20);
        strategy.setExitTime(15, 25);

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
        // Closest Premium 100..400 step 25 x leg SL% 15..85 step 5
        // (applied to every leg; use s.getListOfLegConfigs().get(i) for a specific leg)
        List<ParameterSweep<?>> sweeps = List.of(
                ParameterSweep.numericRange("ClosestPremium", 20, 100, 5,
                        (s, v) -> s.forEachLeg(l -> l.setClosestPremium(v))),
                ParameterSweep.numericRange("LegStopLossPct", 10, 90, 5,
                        (s, v) -> s.forEachLeg(l -> l.setStopLoss(LegTgtSLType.PERCENTAGE, v))));

        // ---------- run ----------
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path output = Path.of("algotest-results",
                "algotest-" + strategy.getTicker().name() + "-" + timestamp + ".csv");

        AlgoTestClient client = new AlgoTestClient(COOKIE.strip());
        new AlgoTestOptimizer(client)
                .slippagePct(0.5)                                   // 0.5% slippage per side
                .costConfig(CostConfig.taxesPlusPerOrder(20))       // taxes + Rs20/order + 18% GST
                .lotSize(10)                                        // SENSEX lot size
                .dteSets(List.of(Set.of(), Set.of(0), Set.of(0, 1)))// all / 0DTE / 0+1 DTE
                .marginEstimate(true)                               // expiry-day margin per config
                .optimize(template, sweeps, output);
        System.out.println("Results written to " + output.toAbsolutePath());
    }
}

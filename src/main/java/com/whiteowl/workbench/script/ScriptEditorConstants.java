package com.whiteowl.workbench.script;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScriptEditorConstants {

    public static final String DEFAULT_INDICATOR_NAME = "New Indicator";

    public static final String DEFAULT_SCREENER_NAME = "New Screener";

    public static final String DEFAULT_RANKER_NAME = "New Ranker";

    public static final String INDICATOR_DSL_REFERENCE = """
            Arrays: open, high, low, close, volume, timestamps
            Property: barCount

            User inputs:
              input(name, default)         input(name, default, min, max)
              input(name, default, min, max, step)

            Indicators:
              sma(source, period)          ema(source, period)
              rsi(source, period)          atr(period)
              macd(source, fast, slow, sig)  → [macd, signal, histogram]
              crossover(a, b)              crossunder(a, b)
              highest(source, period)      lowest(source, period)
              stochastics(kPeriod, dPeriod) obv()
              stddev(source, period)       roc(source, period)
              adx(period)                  supertrend(period, multiplier)
              bollingerBands(source, period, mult)
              cci(period)                  williamsR(period)
              stochRsi(rsi, stoch, kSmooth, dSmooth)
              ultimateOscillator(p1, p2, p3)
              parabolicSar(afStart, afStep, afMax)
              ichimoku(tenkan, kijun, senkouB, disp)
              aroon(period)                keltnerChannels(ema, atr, mult)
              donchianChannels(period)     accumulationDistribution()
              cmf(period)                  mfi(period)
              vwap()                       beta(source, benchmark, period)

            Return: float[] (indicator values per bar)""";

    public static final String SCREENER_DSL_REFERENCE = """
            Arrays: open, high, low, close, volume, timestamps
            Property: barCount

            User inputs:
              input(name, default)         input(name, default, min, max)
              input(name, default, min, max, step)

            Indicators:
              sma(source, period)          ema(source, period)
              rsi(source, period)          atr(period)
              macd(source, fast, slow, sig)  → [macd, signal, histogram]
              crossover(a, b)              crossunder(a, b)
              highest(source, period)      lowest(source, period)
              stochastics(kPeriod, dPeriod) obv()
              stddev(source, period)       roc(source, period)
              adx(period)                  supertrend(period, multiplier)
              bollingerBands(source, period, mult)
              cci(period)                  williamsR(period)
              stochRsi(rsi, stoch, kSmooth, dSmooth)
              ultimateOscillator(p1, p2, p3)
              parabolicSar(afStart, afStep, afMax)
              ichimoku(tenkan, kijun, senkouB, disp)
              aroon(period)                keltnerChannels(ema, atr, mult)
              donchianChannels(period)     accumulationDistribution()
              cmf(period)                  mfi(period)
              vwap()                       beta(source, benchmark, period)

            Return: boolean (true if scrip matches the screen)""";

    public static final String DEFAULT_STRATEGY_NAME = "New Strategy";

    public static final String RANKER_DSL_REFERENCE = """
            Arrays: open, high, low, close, volume, timestamps
            Property: barCount

            User inputs:
              input(name, default)         input(name, default, min, max)
              input(name, default, min, max, step)

            Indicators:
              sma(source, period)          ema(source, period)
              rsi(source, period)          atr(period)
              macd(source, fast, slow, sig)  → [macd, signal, histogram]
              crossover(a, b)              crossunder(a, b)
              highest(source, period)      lowest(source, period)
              stochastics(kPeriod, dPeriod) obv()
              stddev(source, period)       roc(source, period)
              adx(period)                  supertrend(period, multiplier)
              bollingerBands(source, period, mult)
              cci(period)                  williamsR(period)
              stochRsi(rsi, stoch, kSmooth, dSmooth)
              ultimateOscillator(p1, p2, p3)
              parabolicSar(afStart, afStep, afMax)
              ichimoku(tenkan, kijun, senkouB, disp)
              aroon(period)                keltnerChannels(ema, atr, mult)
              donchianChannels(period)     accumulationDistribution()
              cmf(period)                  mfi(period)
              vwap()                       beta(source, benchmark, period)

            Return: Number (rank score, higher = better)
            Return null to exclude the scrip from ranking""";

    public static final String STRATEGY_DSL_REFERENCE = """
            Arrays: open, high, low, close, volume, timestamps
            Property: barCount

            User inputs:
              input(name, default)         input(name, default, min, max)
              input(name, default, min, max, step)

            Indicators:
              sma(source, period)          ema(source, period)
              rsi(source, period)          atr(period)
              macd(source, fast, slow, sig)  → [macd, signal, histogram]
              crossover(a, b)              crossunder(a, b)
              highest(source, period)      lowest(source, period)
              stddev(source, period)       roc(source, period)
              adx(period)                  bollinger(source, period, mult)
              volumeSma(period)            swingHighLow(strength)

            Signals:
              longEntry(qty)               longExit()
              shortEntry(qty)              shortExit()
              closePosition(posId)

            Portfolio:
              getCash()  getEquity()  hasOpenPositions()
              positionCount()  avgPrice()  unrealizedPnl()""";

}

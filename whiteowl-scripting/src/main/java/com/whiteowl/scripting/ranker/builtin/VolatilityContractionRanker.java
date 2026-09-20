package com.whiteowl.scripting.ranker.builtin;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.indicator.Atr;
import com.whiteowl.core.indicator.Rsi;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.scripting.ranker.Ranker;
import com.whiteowl.scripting.ranker.RankerSetting;

import java.util.List;
import java.util.Map;

/**
 * Ranks scrips by how much their volatility has contracted over the recent {@code Period} bars.
 * <p>
 * Normalized volatility per bar = ((StdDev(close, period) + ATR(period)) / 2) / SMA(close, period).
 * The RSI (with the same {@code period}) of that normalized-volatility series is then computed;
 * a lower RSI indicates stronger contraction. The final RSI value is returned as the rank, so
 * sorting ascending puts the most-contracted scrips at the top.
 */
public final class VolatilityContractionRanker implements Ranker {

    private static final String RANKER_NAME = "Volatility Contraction";
    private static final String PERIOD_SETTING = "Period";
    private static final int DEFAULT_PERIOD = 5;

    private int period = DEFAULT_PERIOD;

    @Override
    public String getName() {
        return RANKER_NAME;
    }

    @Override
    public List<RankerSetting> getSettings() {
        return List.of(new RankerSetting(PERIOD_SETTING, Integer.class, DEFAULT_PERIOD));
    }

    @Override
    public Map<String, Object> getSettingValues() {
        return Map.of(PERIOD_SETTING, period);
    }

    @Override
    public void updateSetting(String name, Object value) {
        if (PERIOD_SETTING.equals(name) && value instanceof Number n) {
            period = Math.max(2, n.intValue());
        }
    }

    @Override
    public Double rank(Scrip scrip, Bars bars) {
        int size = bars.size();
        // Need enough bars for a rolling window plus RSI seeding.
        int minRequired = period * 2 + 1;
        if (size < minRequired) return null;

        float[] high = new float[size];
        float[] low = new float[size];
        float[] close = new float[size];
        for (int i = 0; i < size; i++) {
            high[i] = bars.getHigh(i);
            low[i] = bars.getLow(i);
            close[i] = bars.getClose(i);
        }

        float[] atr = Atr.compute(high, low, close, size, period);
        float[] normVol = new float[size];
        for (int i = 0; i < size; i++) {
            normVol[i] = Float.NaN;
        }
        for (int i = period - 1; i < size; i++) {
            float mean = 0f;
            for (int j = i - period + 1; j <= i; j++) mean += close[j];
            mean /= period;
            if (mean <= 0f) continue;
            float variance = 0f;
            for (int j = i - period + 1; j <= i; j++) {
                float d = close[j] - mean;
                variance += d * d;
            }
            float stdDev = (float) Math.sqrt(variance / period);
            float atrValue = atr[i];
            if (Float.isNaN(atrValue)) continue;
            normVol[i] = ((stdDev + atrValue) / 2f) / mean;
        }

        // Compact the leading NaNs so Rsi has a contiguous series to work on.
        int firstValid = 0;
        while (firstValid < size && Float.isNaN(normVol[firstValid])) firstValid++;
        int validLen = size - firstValid;
        if (validLen < period + 1) return null;
        float[] source = new float[validLen];
        System.arraycopy(normVol, firstValid, source, 0, validLen);

        float[] rsi = Rsi.compute(source, validLen, period);
        float last = rsi[validLen - 1];
        if (Float.isNaN(last)) return null;
        return (double) last;
    }

}

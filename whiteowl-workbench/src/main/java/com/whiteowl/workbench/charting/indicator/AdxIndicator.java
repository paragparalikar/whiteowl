package com.whiteowl.workbench.charting.indicator;

import com.whiteowl.core.bar.model.Bars;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

public final class AdxIndicator implements SubChartIndicator {

    private static final String INDICATOR_NAME = "ADX";
    private static final String PERIOD_SETTING = "Period";
    private static final int DEFAULT_PERIOD = 14;
    private static final double ADX_MIN = 0;
    private static final double ADX_MAX = 100;
    private static final double TREND_THRESHOLD = 25;
    private static final Color ADX_COLOR = Color.web("#f0b90b");
    private static final Color PLUS_DI_COLOR = Color.web("#26a69a");
    private static final Color MINUS_DI_COLOR = Color.web("#ef5350");
    private static final double HUNDRED = 100.0;

    @Override
    public String getName() {
        return INDICATOR_NAME;
    }

    @Override
    public List<IndicatorSetting> getSettings() {
        return List.of(new IndicatorSetting(PERIOD_SETTING, Integer.class, DEFAULT_PERIOD));
    }

    @Override
    public SubChartResult compute(Bars bars, Map<String, Object> settings) {
        int period = resolveInt(settings, PERIOD_SETTING, DEFAULT_PERIOD);
        int size = bars.size();
        double[] adxValues = new double[size];
        double[] plusDiValues = new double[size];
        double[] minusDiValues = new double[size];
        int required = period * 2 + 1;
        if (size < required) {
            initializeEmpty(adxValues, size);
            initializeEmpty(plusDiValues, size);
            initializeEmpty(minusDiValues, size);
        } else {
            computeAdx(bars, adxValues, plusDiValues, minusDiValues, period, size);
        }
        String label = INDICATOR_NAME + "(" + period + ")";
        List<SubChartResult.ExtraSeries> extras = List.of(
                new SubChartResult.ExtraSeries(plusDiValues, PLUS_DI_COLOR),
                new SubChartResult.ExtraSeries(minusDiValues, MINUS_DI_COLOR));
        return new SubChartResult(adxValues, ADX_COLOR, label,
                ADX_MIN, ADX_MAX, extras, TREND_THRESHOLD);
    }

    private void computeAdx(Bars bars, double[] adx, double[] plusDi, double[] minusDi,
                            int period, int size) {
        initializeEmpty(adx, period);
        initializeEmpty(plusDi, period);
        initializeEmpty(minusDi, period);
        double smoothedPlusDm = 0;
        double smoothedMinusDm = 0;
        double smoothedTr = 0;
        for (int i = 1; i <= period; i++) {
            smoothedTr += trueRange(bars, i);
            smoothedPlusDm += plusDm(bars, i);
            smoothedMinusDm += minusDm(bars, i);
        }
        plusDi[period] = smoothedTr == 0 ? 0 : (smoothedPlusDm / smoothedTr) * HUNDRED;
        minusDi[period] = smoothedTr == 0 ? 0 : (smoothedMinusDm / smoothedTr) * HUNDRED;
        adx[period] = Double.NaN;
        double adxSum = 0;
        for (int i = period + 1; i < size; i++) {
            smoothedTr = smoothedTr - (smoothedTr / period) + trueRange(bars, i);
            smoothedPlusDm = smoothedPlusDm - (smoothedPlusDm / period) + plusDm(bars, i);
            smoothedMinusDm = smoothedMinusDm - (smoothedMinusDm / period) + minusDm(bars, i);
            double pdi = smoothedTr == 0 ? 0 : (smoothedPlusDm / smoothedTr) * HUNDRED;
            double mdi = smoothedTr == 0 ? 0 : (smoothedMinusDm / smoothedTr) * HUNDRED;
            plusDi[i] = pdi;
            minusDi[i] = mdi;
            double diSum = pdi + mdi;
            double dx = diSum == 0 ? 0 : (Math.abs(pdi - mdi) / diSum) * HUNDRED;
            if (i < period * 2) {
                adxSum += dx;
                adx[i] = Double.NaN;
            } else if (i == period * 2) {
                adxSum += dx;
                adx[i] = adxSum / period;
            } else {
                adx[i] = (adx[i - 1] * (period - 1) + dx) / period;
            }
        }
    }

    private double trueRange(Bars bars, int i) {
        double hl = bars.getHigh(i) - bars.getLow(i);
        double hc = Math.abs(bars.getHigh(i) - bars.getClose(i - 1));
        double lc = Math.abs(bars.getLow(i) - bars.getClose(i - 1));
        return Math.max(hl, Math.max(hc, lc));
    }

    private double plusDm(Bars bars, int i) {
        double upMove = bars.getHigh(i) - bars.getHigh(i - 1);
        double downMove = bars.getLow(i - 1) - bars.getLow(i);
        return (upMove > downMove && upMove > 0) ? upMove : 0;
    }

    private double minusDm(Bars bars, int i) {
        double upMove = bars.getHigh(i) - bars.getHigh(i - 1);
        double downMove = bars.getLow(i - 1) - bars.getLow(i);
        return (downMove > upMove && downMove > 0) ? downMove : 0;
    }

    private int resolveInt(Map<String, Object> settings, String key, int defaultValue) {
        Object val = settings.get(key);
        if (val instanceof Integer) return (Integer) val;
        return defaultValue;
    }

    private void initializeEmpty(double[] values, int count) {
        for (int i = 0; i < count; i++) {
            values[i] = Double.NaN;
        }
    }

}

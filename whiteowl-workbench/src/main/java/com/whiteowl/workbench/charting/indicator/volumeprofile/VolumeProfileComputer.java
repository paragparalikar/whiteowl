package com.whiteowl.workbench.charting.indicator.volumeprofile;

import com.whiteowl.core.bar.model.Bars;

import java.util.ArrayList;
import java.util.List;

import static com.whiteowl.workbench.charting.indicator.volumeprofile.VolumeProfileCountMode.CLOSE;
import static com.whiteowl.workbench.charting.indicator.volumeprofile.VolumeProfileCountMode.VOLUME;

public final class VolumeProfileComputer {

    private VolumeProfileComputer() {
    }

    public static VolumeProfileData compute(Bars bars, int lookback, float atrMultiple,
                                           VolumeProfileCountMode mode) {
        int size = bars.size();
        int from = Math.max(0, size - lookback);
        return computeRange(bars, from, size, atrMultiple, mode);
    }

    public static VolumeProfileData computeRange(Bars bars, int fromBar, int toBar,
                                                float atrMultiple, VolumeProfileCountMode mode) {
        int size = bars.size();
        int from = Math.max(0, fromBar);
        int to = Math.min(size, toBar);
        if (from >= to) return new VolumeProfileData(List.of(), from, to);
        float atr = computeAtr(bars, from, to);
        if (atr <= 0) return new VolumeProfileData(List.of(), from, to);
        float binSize = atr * atrMultiple;
        if (binSize <= 0) return new VolumeProfileData(List.of(), from, to);
        float lowestLow = Float.MAX_VALUE;
        float highestHigh = -Float.MAX_VALUE;
        for (int i = from; i < to; i++) {
            if (bars.getLow(i) < lowestLow) lowestLow = bars.getLow(i);
            if (bars.getHigh(i) > highestHigh) highestHigh = bars.getHigh(i);
        }
        float rangeStart = (float) (Math.floor(lowestLow / binSize) * binSize);
        int binCount = (int) Math.ceil((highestHigh - rangeStart) / binSize);
        if (binCount <= 0) return new VolumeProfileData(List.of(), from, to);
        long[] counts = new long[binCount];
        for (int i = from; i < to; i++) {
            countBar(bars, i, rangeStart, binSize, binCount, counts, mode);
        }
        List<VolumeProfileBin> bins = new ArrayList<>(binCount);
        for (int b = 0; b < binCount; b++) {
            float lo = rangeStart + b * binSize;
            float hi = lo + binSize;
            bins.add(new VolumeProfileBin(lo, hi, counts[b]));
        }
        return new VolumeProfileData(bins, from, to);
    }

    private static void countBar(Bars bars, int i, float rangeStart, float binSize,
                                 int binCount, long[] counts, VolumeProfileCountMode mode) {
        if (mode == CLOSE) {
            float close = bars.getClose(i);
            int bin = (int) ((close - rangeStart) / binSize);
            if (bin >= 0 && bin < binCount) counts[bin]++;
        } else if (mode == VOLUME) {
            long vol = bars.getVolume(i);
            float low = bars.getLow(i);
            float high = bars.getHigh(i);
            int startBin = Math.max(0, (int) ((low - rangeStart) / binSize));
            int endBin = Math.min(binCount - 1, (int) ((high - rangeStart) / binSize));
            int span = endBin - startBin + 1;
            long perBin = span > 0 ? vol / span : vol;
            for (int b = startBin; b <= endBin; b++) {
                counts[b] += perBin;
            }
        } else {
            float low = bars.getLow(i);
            float high = bars.getHigh(i);
            int startBin = Math.max(0, (int) ((low - rangeStart) / binSize));
            int endBin = Math.min(binCount - 1, (int) ((high - rangeStart) / binSize));
            for (int b = startBin; b <= endBin; b++) {
                counts[b]++;
            }
        }
    }

    private static float computeAtr(Bars bars, int from, int to) {
        int atrPeriod = Math.min(to - from, to - 1);
        if (atrPeriod <= 0) return bars.getHigh(from) - bars.getLow(from);
        int start = Math.max(1, from);
        double sum = 0;
        int count = 0;
        for (int i = start; i < to; i++) {
            double hl = bars.getHigh(i) - bars.getLow(i);
            double hc = Math.abs(bars.getHigh(i) - bars.getClose(i - 1));
            double lc = Math.abs(bars.getLow(i) - bars.getClose(i - 1));
            sum += Math.max(hl, Math.max(hc, lc));
            count++;
        }
        return count > 0 ? (float) (sum / count) : 0;
    }

}

package com.whiteowl.workbench.charting;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.trendline.Trendline;
import com.whiteowl.workbench.charting.indicator.IndicatorResult;
import com.whiteowl.workbench.charting.indicator.volumeprofile.VolumeProfileData;

import java.util.List;

public record ChartContext(
        Bars bars,
        BarDataProvider dataProvider,
        int viewStart,
        int viewEnd,
        double chartWidth,
        double chartHeight,
        double totalWidth,
        double totalHeight,
        float minPrice,
        float maxPrice,
        double barWidth,
        double bodyWidth,
        double mouseX,
        double mouseY,
        Scrip scrip,
        Timeframe timeframe,
        List<IndicatorResult> priceOverlayResults,
        List<IndicatorResult> volumeOverlayResults,
        boolean[] screenMarkers,
        List<Trendline> trendlines,
        VolumeProfileData volumeProfile,
        boolean logScale
) {

    public int translateIndex(int viewIndex) {
        return dataProvider.translateIndex(viewIndex);
    }

    public double toY(double price) {
        if (logScale && minPrice > 0 && price > 0) {
            double logMin = Math.log(minPrice);
            double logMax = Math.log(maxPrice);
            double logSpan = logMax - logMin;
            if (logSpan == 0) logSpan = 1;
            return ChartTheme.PADDING_TOP + chartHeight * (1 - (Math.log(price) - logMin) / logSpan);
        }
        float priceSpan = maxPrice - minPrice;
        if (priceSpan == 0) return ChartTheme.PADDING_TOP;
        return ChartTheme.PADDING_TOP + chartHeight * (1 - (price - minPrice) / priceSpan);
    }

    public double toPrice(double y) {
        double ratio = (ChartTheme.PADDING_TOP + chartHeight - y) / chartHeight;
        if (logScale && minPrice > 0) {
            double logMin = Math.log(minPrice);
            double logMax = Math.log(maxPrice);
            return Math.exp(logMin + ratio * (logMax - logMin));
        }
        return minPrice + ratio * (maxPrice - minPrice);
    }

}

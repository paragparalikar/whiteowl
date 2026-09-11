package com.whiteowl.workbench.bar.download;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.scrip.model.Scrip;

import java.time.ZonedDateTime;

public interface BarDownloadListener {

    void onScripStart(Scrip scrip, Timeframe timeframe, int completed, int total);

    void onForwardComplete(Scrip scrip, Timeframe timeframe, int barCount, ZonedDateTime from, ZonedDateTime to);

    void onBackwardComplete(Scrip scrip, Timeframe timeframe, int barCount, ZonedDateTime from, ZonedDateTime to);

    void onScripComplete(Scrip scrip, Timeframe timeframe, int completed, int total);

    void onScripError(Scrip scrip, Timeframe timeframe, String error);

}

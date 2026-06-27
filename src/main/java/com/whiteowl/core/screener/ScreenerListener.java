package com.whiteowl.core.screener;

import com.whiteowl.core.scrip.model.Scrip;

public interface ScreenerListener {

    void onProgress(int completed, int total);

    void onMatch(Scrip scrip);

    void onError(Scrip scrip, String error);

}

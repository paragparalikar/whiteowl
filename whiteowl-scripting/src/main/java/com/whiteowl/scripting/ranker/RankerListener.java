package com.whiteowl.scripting.ranker;

import com.whiteowl.core.scrip.model.Scrip;

public interface RankerListener {

    void onProgress(int completed, int total);

    void onRanked(Scrip scrip, Double value);

    void onError(Scrip scrip, String error);

}

package com.whiteowl.client.kite.ticker;

import java.util.ArrayList;

import com.whiteowl.client.kite.model.KiteTick;

public interface OnTicks {
    void onTicks(ArrayList<KiteTick> ticks);
}

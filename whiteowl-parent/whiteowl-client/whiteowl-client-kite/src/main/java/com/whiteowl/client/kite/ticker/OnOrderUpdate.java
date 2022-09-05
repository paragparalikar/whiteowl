package com.whiteowl.client.kite.ticker;

import com.whiteowl.client.kite.model.Order;

public interface OnOrderUpdate {
    void onOrderUpdate(Order order);
}

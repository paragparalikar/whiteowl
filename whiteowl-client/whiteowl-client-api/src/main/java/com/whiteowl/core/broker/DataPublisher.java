package com.whiteowl.core.broker;

import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.tick.model.Tick;

public interface DataPublisher {

    void publish(Tick tick);

    void publish(Order order);

}

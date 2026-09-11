package com.whiteowl.client.kite;

import com.whiteowl.client.kite.model.KiteOrder;
import com.whiteowl.client.kite.model.KiteTick;

public interface KiteDataPublisher {

    void publish(KiteTick tick);

    void publish(KiteOrder order);

}

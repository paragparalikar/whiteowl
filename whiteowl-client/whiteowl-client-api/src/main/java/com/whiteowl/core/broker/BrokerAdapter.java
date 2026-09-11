package com.whiteowl.core.broker;

import com.whiteowl.client.api.Candle;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.OcoGttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.order.model.OrderUpdateListener;
import com.whiteowl.core.portfolio.model.Funds;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.tick.model.TickListener;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

public interface BrokerAdapter extends AutoCloseable {

    void init();

    void subscribe(Collection<Scrip> scrips);

    List<Candle> fetchHistoricalData(Scrip scrip, Timeframe timeframe, ZonedDateTime from, ZonedDateTime to);

    boolean hasInstrumentMapping(Scrip scrip);

    Order createOrder(Order order);

    Order updateOrder(Order order);

    Order cancelOrder(Order order);

    List<Order> fetchOrders();

    List<GttOrder> fetchGtts();

    GttOrder createGtt(GttOrder gtt);

    GttOrder updateGtt(GttOrder gtt);

    void cancelGtt(int gttId);

    List<OcoGttOrder> fetchOcoGtts();

    OcoGttOrder createOcoGtt(OcoGttOrder oco);

    OcoGttOrder updateOcoGtt(OcoGttOrder oco);

    List<Holding> fetchHoldings();

    List<Position> fetchPositions();

    Funds fetchFunds();

    float fetchEquityMargin();

    float fetchCommodityMargin();

    default void addTickListener(TickListener listener) {}

    default void removeTickListener(TickListener listener) {}

    default void addOrderUpdateListener(OrderUpdateListener listener) {}

    default void removeOrderUpdateListener(OrderUpdateListener listener) {}

}

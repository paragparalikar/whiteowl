package com.whiteowl.client.kite;

import com.whiteowl.client.kite.model.KiteCandle;
import com.whiteowl.client.kite.model.KiteGttCondition;
import com.whiteowl.client.kite.model.KiteGttOrder;
import com.whiteowl.client.kite.model.KiteGttTrigger;
import com.whiteowl.client.kite.model.KiteGttTriggerId;
import com.whiteowl.client.kite.model.KiteGttType;
import com.whiteowl.client.kite.model.KiteHolding;
import com.whiteowl.client.kite.model.KiteInterval;
import com.whiteowl.client.kite.model.KiteMargin;
import com.whiteowl.client.kite.model.KiteOrder;
import com.whiteowl.client.kite.model.KiteOrderId;
import com.whiteowl.client.kite.model.KitePosition;
import com.whiteowl.client.kite.model.KiteProfile;
import com.whiteowl.client.kite.model.KiteQuote;
import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.client.kite.model.KiteSymbol;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface KiteApi extends AutoCloseable {

    void init();

    List<KiteCandle> getHistoricalData(KiteSymbol symbol, KiteInterval interval, ZonedDateTime from, ZonedDateTime to);

    KiteProfile getProfile();

    KiteMargin getMargin();

    List<KiteHolding> getHoldings();

    List<KitePosition> getPositions();

    List<KiteOrder> getOrders();

    KiteOrderId createOrder(KiteOrder order);

    KiteOrderId updateOrder(KiteOrder order);

    KiteOrderId cancelOrder(KiteOrder order);

    Map<String, KiteQuote> getQuotes(Collection<KiteSymbol> instruments, KiteQuoteMode mode);

    void subscribe(Collection<KiteSymbol> symbols);

    List<KiteGttTrigger> getGttTriggers();

    KiteGttTrigger getGttTrigger(int triggerId);

    KiteGttTriggerId createGttTrigger(KiteGttCondition condition, List<KiteGttOrder> orders, KiteGttType type, String expiresAt);

    KiteGttTriggerId updateGttTrigger(int triggerId, KiteGttCondition condition, List<KiteGttOrder> orders, KiteGttType type, String expiresAt);

    KiteGttTriggerId deleteGttTrigger(int triggerId);

}

package com.whiteowl.client.kite;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

import com.whiteowl.client.kite.model.CandleSeries;
import com.whiteowl.client.kite.model.Holding;
import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.client.kite.model.KiteQuote;
import com.whiteowl.client.kite.model.KiteQuoteMode;
import com.whiteowl.client.kite.model.Margin;
import com.whiteowl.client.kite.model.Order;
import com.whiteowl.client.kite.model.OrderId;
import com.whiteowl.client.kite.model.OrderType;
import com.whiteowl.client.kite.model.OrderValidity;
import com.whiteowl.client.kite.model.OrderVariety;
import com.whiteowl.client.kite.model.Position;
import com.whiteowl.client.kite.model.Profile;

public interface KiteConnectApi {

	CandleSeries getData(long instrumentToken, String interval, ZonedDateTime from, ZonedDateTime to);

	Profile getProfile();

	Margin getMargin();

	List<Holding> getHoldings();

	List<Position> getPositions();

	List<Order> getOrders();

	OrderId createOrder(Order order);

	OrderId update(Order order);

	OrderId update(OrderVariety variety, String orderId, OrderType orderType, int quantity, OrderValidity validity);

	OrderId cancel(Order order);

	OrderId cancel(OrderVariety variety, String orderId);

	Collection<KiteQuote> getQuotes(Collection<Instrument> instruments, KiteQuoteMode mode);
}
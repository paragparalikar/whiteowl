package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.api.Candle;
import com.whiteowl.client.kite.KiteApi;
import com.whiteowl.client.kite.model.KiteCandle;
import com.whiteowl.client.kite.model.KiteGttCondition;
import com.whiteowl.client.kite.model.KiteGttOrder;
import com.whiteowl.client.kite.model.KiteGttType;
import com.whiteowl.client.kite.model.KiteInterval;
import com.whiteowl.client.kite.model.KiteSymbol;
import com.whiteowl.core.broker.BrokerAdapter;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.OcoGttOrder;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.order.model.OrderUpdateListener;
import com.whiteowl.core.portfolio.model.Funds;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.portfolio.model.Position;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.core.tick.model.Tick;
import com.whiteowl.core.tick.model.TickListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RequiredArgsConstructor
public final class KiteBrokerAdapter implements BrokerAdapter {

    private final KiteApi kiteApi;
    private final String portfolioId;
    private final KiteMapper mapper;
    private final List<TickListener> tickListeners = new CopyOnWriteArrayList<>();
    private final List<OrderUpdateListener> orderUpdateListeners = new CopyOnWriteArrayList<>();
    private volatile boolean consumersWired;

    public KiteBrokerAdapter(KiteApi kiteApi, String portfolioId) {
        this(kiteApi, portfolioId, KiteMapper.INSTANCE);
    }

    @Override
    public void init() {
        kiteApi.init();
        wireConsumers();
    }

    @Override
    public void close() throws Exception {
        kiteApi.close();
    }

    @Override
    public void addTickListener(TickListener listener) {
        tickListeners.add(listener);
        wireConsumers();
    }

    @Override
    public void removeTickListener(TickListener listener) {
        tickListeners.remove(listener);
    }

    @Override
    public void addOrderUpdateListener(OrderUpdateListener listener) {
        orderUpdateListeners.add(listener);
        wireConsumers();
    }

    @Override
    public void removeOrderUpdateListener(OrderUpdateListener listener) {
        orderUpdateListeners.remove(listener);
    }

    private void wireConsumers() {
        if (consumersWired) return;
        consumersWired = true;
        kiteApi.addTickConsumer(kiteTick -> {
            Tick tick = mapper.toTick(kiteTick);
            if (tick != null) {
                for (TickListener listener : tickListeners) {
                    try {
                        listener.onTick(tick);
                    } catch (Exception e) {
                        log.warn("Error in tick listener", e);
                    }
                }
            }
        });
        kiteApi.addOrderConsumer(kiteOrder -> {
            Order order = mapper.toOrder(kiteOrder);
            if (order != null) {
                for (OrderUpdateListener listener : orderUpdateListeners) {
                    try {
                        listener.onOrderUpdate(order);
                    } catch (Exception e) {
                        log.warn("Error in order update listener", e);
                    }
                }
            }
        });
    }

    @Override
    public void subscribe(Collection<Scrip> scrips) {
        List<KiteSymbol> kiteSymbols = scrips.stream()
                .map(mapper::getKiteSymbol)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (!kiteSymbols.isEmpty()) {
            kiteApi.subscribe(kiteSymbols);
        }
    }

    @Override
    public Order createOrder(Order order) {
        String id = kiteApi.createOrder(mapper.toKiteOrder(order)).getOrderId();
        order.setExchangeOrderId(id);
        return order;
    }

    @Override
    public Order updateOrder(Order order) {
        kiteApi.updateOrder(mapper.toKiteOrder(order));
        return order;
    }

    @Override
    public Order cancelOrder(Order order) {
        kiteApi.cancelOrder(mapper.toKiteOrder(order));
        return order;
    }

    @Override
    public List<Order> fetchOrders() {
        return kiteApi.getOrders().stream()
                .map(mapper::toOrder)
                .toList();
    }

    @Override
    public List<Holding> fetchHoldings() {
        return kiteApi.getHoldings().stream()
                .map(h -> mapper.toHolding(portfolioId, h))
                .toList();
    }

    @Override
    public List<Position> fetchPositions() {
        return kiteApi.getPositions().stream()
                .map(mapper::toPosition)
                .toList();
    }

    @Override
    public Funds fetchFunds() {
        return mapper.toFunds(kiteApi.getMargin());
    }

    public List<Scrip> mapScrips(Collection<KiteSymbol> kiteSymbols) {
        return kiteSymbols.stream().map(mapper::toScrip).toList();
    }

    @Override
    public boolean hasInstrumentMapping(Scrip scrip) {
        return mapper.getKiteSymbol(scrip) != null;
    }

    @Override
    public List<Candle> fetchHistoricalData(Scrip scrip, Timeframe timeframe, ZonedDateTime from, ZonedDateTime to) {
        KiteSymbol kiteSymbol = mapper.getKiteSymbol(scrip);
        if (kiteSymbol == null) return List.of();
        KiteInterval interval = mapper.toKiteInterval(timeframe);
        List<KiteCandle> kiteCandles = fetchBatchedData(kiteSymbol, interval, from, to);
        return kiteCandles.stream()
                .map(kc -> new Candle(kc.getTimestamp(), kc.getOpen(), kc.getHigh(),
                        kc.getLow(), kc.getClose(), kc.getVolume()))
                .toList();
    }

    @Override
    public float fetchEquityMargin() {
        return (float) kiteApi.getMargin().getEquity().getAvailable().getCash();
    }

    @Override
    public float fetchCommodityMargin() {
        return (float) kiteApi.getMargin().getCommodity().getAvailable().getCash();
    }

    public Tick mapTick(com.whiteowl.client.kite.model.KiteTick kiteTick) {
        return mapper.toTick(kiteTick);
    }

    public Order mapOrder(com.whiteowl.client.kite.model.KiteOrder kiteOrder) {
        return mapper.toOrder(kiteOrder);
    }

    @Override
    public List<GttOrder> fetchGtts() {
        return kiteApi.getGttTriggers().stream()
                .filter(t -> t.getType() == KiteGttType.SINGLE || t.getType() == KiteGttType.TRAILING_SINGLE)
                .map(mapper::toGttOrder)
                .toList();
    }

    @Override
    public List<OcoGttOrder> fetchOcoGtts() {
        return kiteApi.getGttTriggers().stream()
                .filter(t -> t.getType() == KiteGttType.TWO_LEG || t.getType() == KiteGttType.TRAILING_TWO_LEG)
                .map(mapper::toOcoGttOrder)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public GttOrder createGtt(GttOrder gtt) {
        Scrip scrip = resolveScrip(gtt.getScripId());
        KiteSymbol kiteSymbol = scrip != null ? mapper.getKiteSymbol(scrip) : null;
        KiteGttCondition condition = mapper.toKiteGttCondition(gtt, scrip, kiteSymbol);
        List<KiteGttOrder> orders = List.of(mapper.toKiteGttOrder(gtt, scrip));
        KiteGttType type = gtt.getTrailingPoints() > 0 ? KiteGttType.TRAILING_SINGLE : KiteGttType.SINGLE;
        int triggerId = kiteApi.createGttTrigger(condition, orders, type, gtt.getExpiresAt()).getTriggerId();
        gtt.setId(triggerId);
        return gtt;
    }

    @Override
    public GttOrder updateGtt(GttOrder gtt) {
        Scrip scrip = resolveScrip(gtt.getScripId());
        KiteSymbol kiteSymbol = scrip != null ? mapper.getKiteSymbol(scrip) : null;
        KiteGttCondition condition = mapper.toKiteGttCondition(gtt, scrip, kiteSymbol);
        List<KiteGttOrder> orders = List.of(mapper.toKiteGttOrder(gtt, scrip));
        KiteGttType type = gtt.getTrailingPoints() > 0 ? KiteGttType.TRAILING_SINGLE : KiteGttType.SINGLE;
        kiteApi.updateGttTrigger(gtt.getId(), condition, orders, type, gtt.getExpiresAt());
        return gtt;
    }

    @Override
    public void cancelGtt(int gttId) {
        kiteApi.deleteGttTrigger(gttId);
    }

    @Override
    public OcoGttOrder createOcoGtt(OcoGttOrder oco) {
        Scrip scrip = resolveScrip(oco.getScripId());
        KiteSymbol kiteSymbol = scrip != null ? mapper.getKiteSymbol(scrip) : null;
        KiteGttCondition condition = mapper.toKiteOcoCondition(oco, scrip, kiteSymbol);
        List<KiteGttOrder> orders = mapper.toKiteOcoOrders(oco, scrip);
        KiteGttType type = oco.getTrailingPoints() > 0 ? KiteGttType.TRAILING_TWO_LEG : KiteGttType.TWO_LEG;
        int triggerId = kiteApi.createGttTrigger(condition, orders, type, oco.getExpiresAt()).getTriggerId();
        oco.setId(triggerId);
        return oco;
    }

    @Override
    public OcoGttOrder updateOcoGtt(OcoGttOrder oco) {
        Scrip scrip = resolveScrip(oco.getScripId());
        KiteSymbol kiteSymbol = scrip != null ? mapper.getKiteSymbol(scrip) : null;
        KiteGttCondition condition = mapper.toKiteOcoCondition(oco, scrip, kiteSymbol);
        List<KiteGttOrder> orders = mapper.toKiteOcoOrders(oco, scrip);
        KiteGttType type = oco.getTrailingPoints() > 0 ? KiteGttType.TRAILING_TWO_LEG : KiteGttType.TWO_LEG;
        kiteApi.updateGttTrigger(oco.getId(), condition, orders, type, oco.getExpiresAt());
        return oco;
    }

    private Scrip resolveScrip(String scripId) {
        if (scripId == null) return null;
        int colonIndex = scripId.indexOf(':');
        if (colonIndex < 0) return null;
        String exchangeName = scripId.substring(0, colonIndex);
        String symbol = scripId.substring(colonIndex + 1);
        return mapper.getScrip(
                mapper.toKiteExchange(com.whiteowl.core.scrip.model.Exchange.valueOf(exchangeName)),
                symbol);
    }

    private List<KiteCandle> fetchBatchedData(KiteSymbol symbol, KiteInterval interval, ZonedDateTime from, ZonedDateTime to) {
        List<KiteCandle> candles = new ArrayList<>();
        ZonedDateTime cursor = to;
        while (cursor != null && cursor.isAfter(from)) {
            ZonedDateTime batchFrom = cursor.minus(Duration.ofDays(interval.getHistoricalBatchLimitInDays()));
            ZonedDateTime effectiveFrom = from.isAfter(batchFrom) ? from : batchFrom;
            List<KiteCandle> batch = kiteApi.getHistoricalData(symbol, interval, effectiveFrom, cursor);
            if (batch != null && !batch.isEmpty()) {
                candles.addAll(batch);
                long earliestTs = batch.stream().mapToLong(KiteCandle::getTimestamp).min().orElse(0);
                ZonedDateTime nextCursor = ZonedDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(earliestTs - 1),
                        from.getZone());
                if (!nextCursor.isBefore(cursor)) break;
                cursor = nextCursor;
            } else {
                cursor = null;
            }
        }
        candles.sort(Comparator.comparingLong(KiteCandle::getTimestamp));
        return candles;
    }

}

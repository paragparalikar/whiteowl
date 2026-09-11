package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteGttCondition;
import com.whiteowl.client.kite.model.KiteGttOrder;
import com.whiteowl.client.kite.model.KiteGttStatus;
import com.whiteowl.client.kite.model.KiteGttTrigger;
import com.whiteowl.client.kite.model.KiteGttType;
import com.whiteowl.client.kite.model.KiteSymbol;
import com.whiteowl.core.gtt.model.GttOrder;
import com.whiteowl.core.gtt.model.GttStatus;
import com.whiteowl.core.gtt.model.OcoGttOrder;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
final class KiteGttMapper {

    private final KiteScripMapper scripMapper;
    private final KiteExchangeMapper exchangeMapper;
    private final KiteProductMapper productMapper;
    private final KiteLimitTypeMapper limitTypeMapper;
    private final KiteOrderSideMapper orderSideMapper;

    public GttOrder toGttOrder(KiteGttTrigger trigger) {
        if (trigger == null) return null;
        KiteGttCondition condition = trigger.getCondition();
        KiteGttOrder kiteOrder = trigger.getOrders().isEmpty() ? null : trigger.getOrders().getFirst();
        if (condition == null || kiteOrder == null) return null;
        Scrip scrip = scripMapper.getScrip(condition.getExchange(), condition.getTradingsymbol());
        String scripId = scrip != null ? scrip.getId() : buildFallbackScripId(condition);
        float triggerPrice = condition.getTriggerValues().length > 0 ? condition.getTriggerValues()[0] : 0;
        float trailing = extractTrailingPoints(condition);
        return GttOrder.builder()
                .id(trigger.getId())
                .scripId(scripId)
                .side(orderSideMapper.toOrderSide(kiteOrder.getTransactionType()))
                .limitType(limitTypeMapper.toLimitType(kiteOrder.getOrderType()))
                .product(productMapper.toProduct(kiteOrder.getProduct()))
                .quantity(kiteOrder.getQuantity())
                .triggerPrice(triggerPrice)
                .orderPrice(kiteOrder.getPrice())
                .lastPrice(condition.getLastPrice())
                .trailingPoints(trailing)
                .status(toGttStatus(trigger.getStatus()))
                .expiresAt(trigger.getExpiresAt())
                .createdAt(trigger.getCreatedAt())
                .updatedAt(trigger.getUpdatedAt())
                .build();
    }

    public KiteGttCondition toKiteGttCondition(GttOrder gtt, Scrip scrip, KiteSymbol kiteSymbol) {
        return KiteGttCondition.builder()
                .exchange(scrip != null ? exchangeMapper.toKiteExchange(scrip.getExchange()) : null)
                .tradingsymbol(scrip != null ? scrip.getSymbol() : gtt.getScripId())
                .triggerValues(new float[]{gtt.getTriggerPrice()})
                .lastPrice(gtt.getLastPrice())
                .trailingPoints(gtt.getTrailingPoints() > 0 ? new float[]{gtt.getTrailingPoints()} : null)
                .instrumentToken(kiteSymbol != null ? kiteSymbol.getInstrumentToken() : 0)
                .build();
    }

    public KiteGttOrder toKiteGttOrder(GttOrder gtt, Scrip scrip) {
        return KiteGttOrder.builder()
                .exchange(scrip != null ? exchangeMapper.toKiteExchange(scrip.getExchange()) : null)
                .tradingsymbol(scrip != null ? scrip.getSymbol() : gtt.getScripId())
                .transactionType(orderSideMapper.toKiteTransactionType(gtt.getSide()))
                .quantity(gtt.getQuantity())
                .orderType(limitTypeMapper.toKiteLimitType(gtt.getLimitType()))
                .product(productMapper.toKiteProduct(gtt.getProduct()))
                .price(gtt.getOrderPrice())
                .build();
    }

    public OcoGttOrder toOcoGttOrder(KiteGttTrigger trigger) {
        if (trigger == null) return null;
        if (trigger.getType() != KiteGttType.TWO_LEG && trigger.getType() != KiteGttType.TRAILING_TWO_LEG) return null;
        KiteGttCondition condition = trigger.getCondition();
        List<KiteGttOrder> orders = trigger.getOrders();
        if (condition == null || orders == null || orders.size() < 2) return null;
        Scrip scrip = scripMapper.getScrip(condition.getExchange(), condition.getTradingsymbol());
        String scripId = scrip != null ? scrip.getId() : buildFallbackScripId(condition);
        float[] triggerValues = condition.getTriggerValues();
        float slTrigger = triggerValues.length > 0 ? triggerValues[0] : 0;
        float tgtTrigger = triggerValues.length > 1 ? triggerValues[1] : 0;
        KiteGttOrder slOrder = orders.get(0);
        KiteGttOrder tgtOrder = orders.get(1);
        float trailing = extractTrailingPoints(condition);
        return OcoGttOrder.builder()
                .id(trigger.getId())
                .scripId(scripId)
                .side(orderSideMapper.toOrderSide(slOrder.getTransactionType()))
                .limitType(limitTypeMapper.toLimitType(slOrder.getOrderType()))
                .product(productMapper.toProduct(slOrder.getProduct()))
                .quantity(slOrder.getQuantity())
                .stoplossTriggerPrice(slTrigger)
                .stoplossOrderPrice(slOrder.getPrice())
                .targetTriggerPrice(tgtTrigger)
                .targetOrderPrice(tgtOrder.getPrice())
                .lastPrice(condition.getLastPrice())
                .trailingPoints(trailing)
                .status(toGttStatus(trigger.getStatus()))
                .expiresAt(trigger.getExpiresAt())
                .createdAt(trigger.getCreatedAt())
                .updatedAt(trigger.getUpdatedAt())
                .build();
    }

    public KiteGttCondition toKiteOcoCondition(OcoGttOrder oco, Scrip scrip, KiteSymbol kiteSymbol) {
        return KiteGttCondition.builder()
                .exchange(scrip != null ? exchangeMapper.toKiteExchange(scrip.getExchange()) : null)
                .tradingsymbol(scrip != null ? scrip.getSymbol() : oco.getScripId())
                .triggerValues(new float[]{oco.getStoplossTriggerPrice(), oco.getTargetTriggerPrice()})
                .lastPrice(oco.getLastPrice())
                .trailingPoints(oco.getTrailingPoints() > 0 ? new float[]{oco.getTrailingPoints()} : null)
                .instrumentToken(kiteSymbol != null ? kiteSymbol.getInstrumentToken() : 0)
                .build();
    }

    public List<KiteGttOrder> toKiteOcoOrders(OcoGttOrder oco, Scrip scrip) {
        KiteGttOrder slOrder = buildKiteGttOrder(oco, scrip, oco.getStoplossOrderPrice());
        KiteGttOrder tgtOrder = buildKiteGttOrder(oco, scrip, oco.getTargetOrderPrice());
        return List.of(slOrder, tgtOrder);
    }

    private KiteGttOrder buildKiteGttOrder(OcoGttOrder oco, Scrip scrip, float price) {
        return KiteGttOrder.builder()
                .exchange(scrip != null ? exchangeMapper.toKiteExchange(scrip.getExchange()) : null)
                .tradingsymbol(scrip != null ? scrip.getSymbol() : oco.getScripId())
                .transactionType(orderSideMapper.toKiteTransactionType(oco.getSide()))
                .quantity(oco.getQuantity())
                .orderType(limitTypeMapper.toKiteLimitType(oco.getLimitType()))
                .product(productMapper.toKiteProduct(oco.getProduct()))
                .price(price)
                .build();
    }

    private GttStatus toGttStatus(KiteGttStatus kiteStatus) {
        if (kiteStatus == null) return null;
        return switch (kiteStatus) {
            case ACTIVE -> GttStatus.ACTIVE;
            case TRIGGERED -> GttStatus.TRIGGERED;
            case DISABLED -> GttStatus.DISABLED;
            case EXPIRED -> GttStatus.EXPIRED;
            case CANCELLED -> GttStatus.CANCELLED;
            case REJECTED -> GttStatus.REJECTED;
        };
    }

    private float extractTrailingPoints(KiteGttCondition condition) {
        if (condition.getTrailingPoints() != null && condition.getTrailingPoints().length > 0) {
            return condition.getTrailingPoints()[0];
        }
        return 0;
    }

    private String buildFallbackScripId(KiteGttCondition condition) {
        if (condition.getExchange() != null) {
            return condition.getExchange().name() + ":" + condition.getTradingsymbol();
        }
        return condition.getTradingsymbol();
    }

}

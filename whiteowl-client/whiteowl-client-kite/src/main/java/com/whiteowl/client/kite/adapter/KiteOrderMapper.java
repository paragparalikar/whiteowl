package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteOrder;
import com.whiteowl.client.kite.model.KiteSymbol;
import com.whiteowl.core.order.model.Order;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
final class KiteOrderMapper {

    private final KiteScripMapper scripMapper;
    private final KiteExchangeMapper exchangeMapper;
    private final KiteProductMapper productMapper;
    private final KiteLimitTypeMapper limitTypeMapper;
    private final KiteOrderSideMapper orderSideMapper;
    private final KiteOrderStatusMapper orderStatusMapper;
    private final KiteValidityMapper validityMapper;
    private final KiteVarietyMapper varietyMapper;

    public Order toOrder(KiteOrder kiteOrder) {
        if (kiteOrder == null) return null;
        Scrip scrip = scripMapper.getScrip(kiteOrder.getExchange(), kiteOrder.getTradingsymbol());
        return Order.builder()
                .id(kiteOrder.getOrderId())
                .exchangeOrderId(kiteOrder.getExchangeOrderId())
                .portfolioId(kiteOrder.getPlacedBy())
                .scripId(scrip != null ? scrip.getId() : buildFallbackScripId(kiteOrder))
                .side(orderSideMapper.toOrderSide(kiteOrder.getTransactionType()))
                .limitType(limitTypeMapper.toLimitType(kiteOrder.getLimitType()))
                .product(productMapper.toProduct(kiteOrder.getProduct()))
                .variety(varietyMapper.toVariety(kiteOrder.getVariety()))
                .validity(validityMapper.toValidity(kiteOrder.getValidity()))
                .status(orderStatusMapper.toOrderStatus(kiteOrder.getStatus()))
                .message(kiteOrder.getStatusMessage())
                .quantity(kiteOrder.getQuantity())
                .filledQuantity(kiteOrder.getFilledQuantity())
                .cancelledQuantity(kiteOrder.getCancelledQuantity())
                .price(kiteOrder.getPrice())
                .triggerPrice(kiteOrder.getTriggerPrice())
                .averagePrice(kiteOrder.getAveragePrice())
                .build();
    }

    public KiteOrder toKiteOrder(Order order) {
        if (order == null) return null;
        Scrip scrip = findScrip(order.getScripId());
        KiteSymbol kiteSymbol = scrip != null ? scripMapper.getKiteSymbol(scrip) : null;
        String tradingsymbol = kiteSymbol != null ? kiteSymbol.getTradingsymbol() : order.getScripId();
        return KiteOrder.builder()
                .orderId(order.getId())
                .exchange(scrip != null ? exchangeMapper.toKiteExchange(scrip.getExchange()) : null)
                .tradingsymbol(tradingsymbol)
                .transactionType(orderSideMapper.toKiteTransactionType(order.getSide()))
                .limitType(limitTypeMapper.toKiteLimitType(order.getLimitType()))
                .product(productMapper.toKiteProduct(order.getProduct()))
                .variety(varietyMapper.toKiteOrderVariety(order.getVariety()))
                .validity(validityMapper.toKiteOrderValidity(order.getValidity()))
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .triggerPrice(order.getTriggerPrice())
                .build();
    }

    private String buildFallbackScripId(KiteOrder kiteOrder) {
        if (kiteOrder.getExchange() != null) {
            return kiteOrder.getExchange().name() + ":" + kiteOrder.getTradingsymbol();
        }
        return kiteOrder.getTradingsymbol();
    }

    private Scrip findScrip(String scripId) {
        if (scripId == null) return null;
        int colonIndex = scripId.indexOf(':');
        if (colonIndex < 0) return null;
        String exchangeName = scripId.substring(0, colonIndex);
        String symbol = scripId.substring(colonIndex + 1);
        return scripMapper.getScrip(
                exchangeMapper.toKiteExchange(
                        com.whiteowl.core.scrip.model.Exchange.valueOf(exchangeName)),
                symbol);
    }

}

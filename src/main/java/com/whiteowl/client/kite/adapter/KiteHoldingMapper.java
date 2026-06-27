package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteHolding;
import com.whiteowl.core.portfolio.model.Holding;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class KiteHoldingMapper {

    private final KiteScripMapper scripMapper;

    public Holding toHolding(String portfolioId, KiteHolding kiteHolding) {
        if (kiteHolding == null) return null;
        Scrip scrip = scripMapper.getScrip(kiteHolding.getExchange(), kiteHolding.getTradingsymbol());
        return Holding.builder()
                .portfolioId(portfolioId)
                .scripId(scrip != null ? scrip.getId() : buildFallbackScripId(kiteHolding))
                .exchange(kiteHolding.getExchange() != null ? kiteHolding.getExchange().name() : "")
                .isin(kiteHolding.getIsin())
                .product(kiteHolding.getProduct() != null ? kiteHolding.getProduct().name() : "")
                .quantity(kiteHolding.getQuantity())
                .usedQuantity(kiteHolding.getUsedQuantity())
                .t1Quantity(kiteHolding.getT1Quantity())
                .realisedQuantity(kiteHolding.getRealisedQuantity())
                .authorisedQuantity(kiteHolding.getAuthorisedQuantity())
                .openingQuantity(kiteHolding.getOpeningQuantity())
                .shortQuantity(kiteHolding.getShortQuantity())
                .collateralQuantity(kiteHolding.getCollateralQuantity())
                .collateralType(kiteHolding.getCollateralType())
                .discrepancy(kiteHolding.isDiscrepancy())
                .averagePrice(kiteHolding.getAveragePrice())
                .lastPrice(kiteHolding.getLastPrice())
                .closePrice(kiteHolding.getClosePrice())
                .pnl(kiteHolding.getPnl())
                .dayChange(kiteHolding.getDayChange())
                .dayChangePercentage(kiteHolding.getDayChangePercentage())
                .build();
    }

    private String buildFallbackScripId(KiteHolding kiteHolding) {
        if (kiteHolding.getExchange() != null) {
            return kiteHolding.getExchange().name() + ":" + kiteHolding.getTradingsymbol();
        }
        return kiteHolding.getTradingsymbol();
    }

}

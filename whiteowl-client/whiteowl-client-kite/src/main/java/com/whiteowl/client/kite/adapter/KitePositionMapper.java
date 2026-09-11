package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KitePosition;
import com.whiteowl.core.portfolio.model.Position;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class KitePositionMapper {

    private final KiteScripMapper scripMapper;
    private final KiteProductMapper productMapper;

    public Position toPosition(KitePosition kp) {
        if (kp == null) return null;
        Scrip scrip = scripMapper.resolve(
                kp.getInstrumentToken(),
                kp.getExchange(),
                kp.getTradingsymbol());
        if (scrip == null) scrip = buildFallbackScrip(kp);
        return Position.builder()
                .scrip(scrip)
                .product(productMapper.toProduct(kp.getProduct()))
                .quantity(kp.getQuantity())
                .overnightQuantity(kp.getOvernightQuantity())
                .averagePrice(kp.getAveragePrice())
                .lastPrice(kp.getLastPrice())
                .pnl(kp.getPnl())
                .unrealised(kp.getUnrealised())
                .realised(kp.getRealised())
                .buyQuantity(kp.getBuyQuantity())
                .buyPrice(kp.getBuyPrice())
                .sellQuantity(kp.getSellQuantity())
                .sellPrice(kp.getSellPrice())
                .build();
    }

    private Scrip buildFallbackScrip(KitePosition kp) {
        String tradingsymbol = kp.getTradingsymbol();
        return Scrip.builder()
                .id(tradingsymbol)
                .symbol(tradingsymbol)
                .name(tradingsymbol)
                .build();
    }

}

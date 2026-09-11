package com.whiteowl.client.kite.adapter;

import com.whiteowl.client.kite.model.KiteMargin;
import com.whiteowl.client.kite.model.KiteSegmentMargin;
import com.whiteowl.core.portfolio.model.Funds;

final class KiteFundsMapper {

    public Funds toFunds(KiteMargin margin) {
        if (margin == null) return Funds.builder().build();
        Funds.FundsBuilder builder = Funds.builder();
        KiteSegmentMargin equity = margin.getEquity();
        if (equity != null && equity.getAvailable() != null) {
            builder.equityAvailableCash(equity.getAvailable().getCash())
                    .equityAvailableCollateral(equity.getAvailable().getCollateral())
                    .equityAvailableIntradayPayin(equity.getAvailable().getIntradayPayin())
                    .equityOpeningBalance(equity.getAvailable().getOpeningBalance())
                    .equityNet(equity.getNet());
            if (equity.getUtilised() != null) {
                builder.equityUtilisedDebits(equity.getUtilised().getDebits())
                        .equityUtilisedExposure(equity.getUtilised().getExposure())
                        .equityUtilisedSpan(equity.getUtilised().getSpan())
                        .equityUtilisedOptionPremium(equity.getUtilised().getOptionPremium())
                        .equityUtilisedPayout(equity.getUtilised().getPayout());
            }
        }
        KiteSegmentMargin commodity = margin.getCommodity();
        if (commodity != null && commodity.getAvailable() != null) {
            builder.commodityAvailableCash(commodity.getAvailable().getCash())
                    .commodityAvailableCollateral(commodity.getAvailable().getCollateral())
                    .commodityNet(commodity.getNet());
        }
        return builder.build();
    }

}

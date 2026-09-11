package com.whiteowl.core.portfolio.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class Funds {

    private final double equityAvailableCash;
    private final double equityAvailableCollateral;
    private final double equityAvailableIntradayPayin;
    private final double equityOpeningBalance;
    private final double equityNet;
    private final double equityUtilisedDebits;
    private final double equityUtilisedExposure;
    private final double equityUtilisedSpan;
    private final double equityUtilisedOptionPremium;
    private final double equityUtilisedPayout;
    private final double commodityAvailableCash;
    private final double commodityAvailableCollateral;
    private final double commodityNet;

}

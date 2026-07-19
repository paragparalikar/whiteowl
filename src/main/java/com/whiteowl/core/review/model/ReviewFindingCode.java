package com.whiteowl.core.review.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewFindingCode {

    MISSING_STOP_LOSS("No stop loss GTT for held scrip"),
    UNDER_COVERED("Stop loss quantity less than net exposure"),
    OVER_COVERED("Stop loss quantity exceeds net exposure"),
    ORPHAN_GTT("SELL GTT without matching holding or position"),
    WIDE_STOP_LOSS("Stop loss trigger too far from buy price"),
    TIGHT_STOP_LOSS("Stop loss trigger too close to buy price"),
    CONCENTRATED_POSITION("Single scrip exceeds max portfolio concentration"),
    STALE_POSITION("Holding held too long with negative returns"),
    EXPIRING_GTT("GTT approaching expiry date"),
    MISSING_TARGET("No target/profit-booking GTT for held scrip"),
    UNFAVORABLE_RISK_REWARD("Reward-to-risk ratio below minimum threshold");

    private final String description;

}

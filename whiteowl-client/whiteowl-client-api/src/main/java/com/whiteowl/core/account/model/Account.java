package com.whiteowl.core.account.model;

import com.whiteowl.core.portfolio.model.BrokerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public final class Account {

    public static final double DEFAULT_POSITION_SIZE = 100000;
    public static final double DEFAULT_STOP_LOSS_PERCENTAGE = 5.0;
    public static final double DEFAULT_WIDE_STOP_LOSS_PERCENTAGE = 10.0;
    public static final double DEFAULT_TIGHT_STOP_LOSS_PERCENTAGE = 2.0;
    public static final double DEFAULT_MAX_CONCENTRATION_PERCENTAGE = 20.0;
    public static final int DEFAULT_STALE_HOLDING_DAYS = 90;
    public static final int DEFAULT_GTT_EXPIRY_WARNING_DAYS = 7;
    public static final double DEFAULT_MIN_RISK_REWARD_RATIO = 2.0;

    private final String id;
    private String name;
    private BrokerType brokerType;
    private String userId;
    private String password;
    private String pin;
    @Builder.Default private double positionSize = DEFAULT_POSITION_SIZE;
    @Builder.Default private double stopLossPercentage = DEFAULT_STOP_LOSS_PERCENTAGE;
    @Builder.Default private double wideStopLossPercentage = DEFAULT_WIDE_STOP_LOSS_PERCENTAGE;
    @Builder.Default private double tightStopLossPercentage = DEFAULT_TIGHT_STOP_LOSS_PERCENTAGE;
    @Builder.Default private double maxConcentrationPercentage = DEFAULT_MAX_CONCENTRATION_PERCENTAGE;
    @Builder.Default private int staleHoldingDays = DEFAULT_STALE_HOLDING_DAYS;
    @Builder.Default private int gttExpiryWarningDays = DEFAULT_GTT_EXPIRY_WARNING_DAYS;
    @Builder.Default private double minRiskRewardRatio = DEFAULT_MIN_RISK_REWARD_RATIO;

}

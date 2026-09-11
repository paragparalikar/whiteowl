package com.whiteowl.workbench.review.model;

public record ReviewConfig(double wideStopLossPercentage, double tightStopLossPercentage,
                            double maxConcentrationPercentage, int staleHoldingDays,
                            int gttExpiryWarningDays, double minRiskRewardRatio) {

}

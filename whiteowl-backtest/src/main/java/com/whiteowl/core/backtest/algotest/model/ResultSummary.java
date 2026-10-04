package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/** Aggregate performance metrics of one backtest run. */
@Data
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultSummary {
    private Double averageProfitPerLosingTrade;
    private Double averageProfitPerTrade;
    private Double averageProfitPerWinningTrade;
    private Double expectancy;
    private Double losingRatio;
    private Double maximumDrawdown;
    private Integer maximumLosingStreak;
    private Double maximumProfitInSingleTrade;
    private Integer maximumWinningStreak;
    private Double minimumProfitInSingleTrade;
    private Integer numberOfTrades;
    private Double overallProfit;
    private Double returnOverMaximumDrawdown;
    private Double rewardToRiskRatio;
    private Double winningRatio;

    /** Computed locally — not returned by the api. Annualized mean/downside-deviation
     *  of daily net P&L (codebase convention: mean/dd * sqrt(tradingDaysPerYear)). */
    private Double sortinoRatio;
}

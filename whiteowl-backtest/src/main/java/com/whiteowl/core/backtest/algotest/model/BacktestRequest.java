package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * POST /backtest body. Dates are "yyyy-MM-dd" strings.
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class BacktestRequest {

    private String source = "WEB";

    /** Set when re-running a saved strategy; null for ad-hoc backtests. */
    private String strategyId;

    private String name;
    private String startDate;
    private String endDate;
    private AlgoStrategy strategy;
    private BacktestAttributes attributes = new BacktestAttributes();

    public static BacktestRequest of(AlgoStrategy strategy, String startDate, String endDate) {
        BacktestRequest request = new BacktestRequest();
        request.setStrategy(strategy);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        return request;
    }
}

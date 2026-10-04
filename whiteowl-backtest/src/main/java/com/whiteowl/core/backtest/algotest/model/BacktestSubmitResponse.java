package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Response of POST /backtest. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BacktestSubmitResponse {

    @JsonProperty("backtest_id")
    private String backtestId;

    private String msg;
    private String error;
}

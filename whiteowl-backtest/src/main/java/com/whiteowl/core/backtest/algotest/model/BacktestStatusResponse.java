package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Response of GET /backtest_status/{id}: {"status":"running"} or {"status":"completed"}. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BacktestStatusResponse {
    private String status;
    private String error;
    private String msg;

    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return status != null && !isCompleted()
                && !"running".equalsIgnoreCase(status)
                && !"queued".equalsIgnoreCase(status)
                && !"pending".equalsIgnoreCase(status)
                && !"processing".equalsIgnoreCase(status);
    }
}

package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Response of GET /backtest/{id} — the full backtest record including the
 * submitted strategy, run parameters and results.
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BacktestResult {

    private String id;
    private String name;
    private String source;
    private String status;
    private String runTs;
    private Map<String, Object> attributes;
    private Parameters parameters;
    private Reference reference;
    private Results results;
    private AlgoStrategy strategy;

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parameters {
        private String startDate;
        private String endDate;
        private Boolean weeklyOldRegime;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Reference {
        private String portfolioId;
        private String shareableId;
        private String strategyId;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Results {

        /** URL of the per-trade CSV on DigitalOcean spaces. */
        private String resultFilePath;

        private ResultSummary summary;

        /** Per-trade rows — kept for completeness; not persisted to the output CSV. */
        private List<List<Object>> tradeWiseResults;

        private List<Long> tradingDays;

        /** Per-trade ticker labels for multi-ticker (portfolio) backtests. */
        private List<String> tradeWiseResultTickers;
    }
}

package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Models for POST /marginCalcAPI/calculate_margin. */
public final class MarginEstimate {

    private MarginEstimate() {
    }

    /**
     * One position in the margin request. {@code instrumentType} is the
     * leg-kind string: "CE", "PE", "FUT", "FUT_P", "CASH" (CASH requires
     * {@code expiry} = null).
     */
    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    public record Position(String ticker, String expiry, double strike,
                           String instrumentType, int netQty) {

        public static Position option(String ticker, String expiry, double strike,
                                      boolean call, int netQty) {
            return new Position(ticker, expiry, strike, call ? "CE" : "PE", netQty);
        }
    }

    /** Request body — matches the site's calculateMargin call. */
    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    public record Request(Object indexPrices, List<Position> listOfPosition,
                          boolean calculateForExpiryDay) {

        public static Request of(List<Position> positions, boolean forExpiryDay) {
            return new Request(java.util.Map.of(), positions, forExpiryDay);
        }
    }

    /** Response; {@code margin} = FinalSpan + FinalExposure. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        @JsonProperty("FinalSpan")
        private Double finalSpan;
        @JsonProperty("FinalExposure")
        private Double finalExposure;
        @JsonProperty("FinalMargin")
        private Double finalMargin;

        public double margin() {
            if (finalMargin != null) {
                return finalMargin;
            }
            return (finalSpan != null ? finalSpan : 0) + (finalExposure != null ? finalExposure : 0);
        }
    }
}

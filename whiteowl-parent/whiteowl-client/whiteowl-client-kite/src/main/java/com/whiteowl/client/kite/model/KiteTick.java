package com.whiteowl.client.kite.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class KiteTick {

    private String mode;
    private boolean tradable;
    private long token;
    @JsonProperty("lastTradedPrice")
    private double lastTradedPrice;
    @JsonProperty("highPrice")
    private double highPrice;
    @JsonProperty("lowPrice")
    private double lowPrice;
    @JsonProperty("openPrice")
    private double openPrice;
    @JsonProperty("closePrice")
    private double closePrice;
    private double change;
    @JsonProperty("lastTradeQuantity")
    private double lastTradedQuantity;
    @JsonProperty("averageTradePrice")
    private double averageTradePrice;
    @JsonProperty("volumeTradedToday")
    private long volumeTradedToday;
    @JsonProperty("totalBuyQuantity")
    private double totalBuyQuantity;
    @JsonProperty("totalSellQuantity")
    private double totalSellQuantity;
    @JsonProperty("lastTradedTime")
    private Date lastTradedTime;
    private double oi;
    @JsonProperty("openInterestDayHigh")
    private double oiDayHigh;
    @JsonProperty("openInterestDayLow")
    private double oiDayLow;
    @JsonProperty("tickTimestamp")
    private Date tickTimestamp;

    private KiteMarketDepth depth;

}

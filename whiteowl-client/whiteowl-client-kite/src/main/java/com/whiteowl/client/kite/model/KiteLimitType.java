package com.whiteowl.client.kite.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum KiteLimitType {

    MARKET,
    LIMIT,
    SL,

    @JsonProperty("SL-M")
    SLM

}

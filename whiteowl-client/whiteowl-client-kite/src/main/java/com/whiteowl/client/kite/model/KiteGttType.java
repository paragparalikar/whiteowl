package com.whiteowl.client.kite.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum KiteGttType {

    @JsonProperty("single")
    SINGLE,

    @JsonProperty("two-leg")
    TWO_LEG,

    @JsonProperty("trailing-single")
    TRAILING_SINGLE,

    @JsonProperty("trailing-two-leg")
    TRAILING_TWO_LEG

}

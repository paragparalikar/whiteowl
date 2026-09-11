package com.whiteowl.client.kite.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum KiteOrderVariety {

    @JsonProperty("regular")
    REGULAR,

    @JsonProperty("amo")
    AMO,

    @JsonProperty("co")
    CO,

    @JsonProperty("iceberg")
    ICEBERG,

    @JsonProperty("auction")
    AUCTION

}

package com.whiteowl.client.kite.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum KiteGttStatus {

    @JsonProperty("active")
    ACTIVE,

    @JsonProperty("triggered")
    TRIGGERED,

    @JsonProperty("disabled")
    DISABLED,

    @JsonProperty("expired")
    EXPIRED,

    @JsonProperty("cancelled")
    CANCELLED,

    @JsonProperty("rejected")
    REJECTED

}

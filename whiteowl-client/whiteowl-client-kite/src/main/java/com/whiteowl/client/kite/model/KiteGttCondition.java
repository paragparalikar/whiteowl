package com.whiteowl.client.kite.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@Jacksonized
public final class KiteGttCondition {

    private int instrumentToken;
    private KiteExchange exchange;
    private String tradingsymbol;
    private float[] triggerValues;
    private float lastPrice;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private float[] trailingPoints;

}

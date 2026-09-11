package com.whiteowl.core.scrip.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public final class Scrip {

    private final String id;
    private final String symbol;
    private final String name;
    private final Exchange exchange;
    private final ScripType scripType;
    private final float lotSize;
    private final float tickSize;

}

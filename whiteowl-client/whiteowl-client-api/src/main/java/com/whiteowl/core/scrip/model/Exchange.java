package com.whiteowl.core.scrip.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Exchange {

    NSE("NSE", "National Stock Exchange"),
    BSE("BSE", "Bombay Stock Exchange");

    private final String code;
    private final String description;

}

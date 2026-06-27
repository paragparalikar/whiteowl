package com.whiteowl.core.collection;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CollectionType {

    ALL_SCRIPS("All Scrips"),
    WATCHLIST("Watchlists"),
    GROUP("Groups"),
    EXAMPLE_GROUP("Example Groups");

    private final String displayLabel;

}

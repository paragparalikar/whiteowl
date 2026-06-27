package com.whiteowl.core.scrip.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScripFilter {

    private static final Set<ScripType> TRADABLE_TYPES = Set.of(ScripType.EQUITY, ScripType.INDEX);
    private static final String ETF_MARKER = "ETF";
    private static final String BEES_MARKER = "BEES";
    private static final String HYPHEN = "-";

    public static boolean isTradable(Scrip scrip) {
        if (!TRADABLE_TYPES.contains(scrip.getScripType())) return false;
        String symbol = scrip.getSymbol();
        String name = scrip.getName();
        if (symbol.contains(HYPHEN)) return false;
        if (symbol.contains(ETF_MARKER) || symbol.contains(BEES_MARKER)) return false;
        if (name != null && (name.contains(ETF_MARKER) || name.contains(BEES_MARKER))) return false;
        return true;
    }

}

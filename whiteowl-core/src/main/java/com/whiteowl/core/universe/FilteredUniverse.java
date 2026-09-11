package com.whiteowl.core.universe;

import lombok.Getter;

import java.util.*;

/**
 * The tradeable universe: per-date lists of qualifying symbols.
 *
 * <p>Produced by {@link LwmsUniverseFilter}. For each date in the universe's
 * date index, stores the set of symbols that pass all filters (LWMS ranking,
 * hard filters, data cleaning). The RVOL filter further narrows this to the
 * daily "Stocks in Play".</p>
 */
@Getter
public final class FilteredUniverse {

    /** Reference to the underlying universe data. */
    private final UniverseDataFrame universe;

    /**
     * Per-date qualifying symbols.
     * Key = date index (into {@code universe.getDates()}),
     * Value = set of qualifying scrip IDs.
     */
    private final Map<Integer, Set<String>> qualifyingByDate;

    /** Symbols that were excluded by data cleaning (globally, not per-date). */
    private final Set<String> excludedSymbols;

    public FilteredUniverse(UniverseDataFrame universe,
                            Map<Integer, Set<String>> qualifyingByDate,
                            Set<String> excludedSymbols) {
        this.universe = universe;
        this.qualifyingByDate = qualifyingByDate;
        this.excludedSymbols = excludedSymbols;
    }

    /**
     * Get the qualifying symbols for a given date index.
     * @return unmodifiable set of scrip IDs, or empty set if no data for that date
     */
    public Set<String> getSymbols(int dateIndex) {
        Set<String> symbols = qualifyingByDate.get(dateIndex);
        return symbols != null ? Collections.unmodifiableSet(symbols) : Collections.emptySet();
    }

    /**
     * Number of qualifying symbols on the given date.
     */
    public int countOnDate(int dateIndex) {
        Set<String> symbols = qualifyingByDate.get(dateIndex);
        return symbols != null ? symbols.size() : 0;
    }

    /**
     * Check if a symbol qualifies on a given date.
     */
    public boolean qualifies(String scripId, int dateIndex) {
        Set<String> symbols = qualifyingByDate.get(dateIndex);
        return symbols != null && symbols.contains(scripId);
    }

    /** Total number of dates with at least one qualifying symbol. */
    public int activeDateCount() {
        return qualifyingByDate.size();
    }
}

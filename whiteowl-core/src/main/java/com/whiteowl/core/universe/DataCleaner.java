package com.whiteowl.core.universe;

import com.whiteowl.core.bar.model.Bars;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Removes symbols that don't meet minimum data quality requirements.
 *
 * <p>Criteria:</p>
 * <ul>
 *   <li>Minimum bar count (default: 252 = 1 year of trading days)</li>
 *   <li>Maximum zero-volume day percentage (default: 5%)</li>
 * </ul>
 */
@Slf4j
public final class DataCleaner {

    private final int minBars;
    private final float maxZeroVolumePct;

    public DataCleaner() {
        this(252, 0.05f);
    }

    public DataCleaner(int minBars, float maxZeroVolumePct) {
        this.minBars = minBars;
        this.maxZeroVolumePct = maxZeroVolumePct;
    }

    /**
     * Filter out symbols with insufficient or poor-quality data.
     *
     * @param universe the loaded universe data frame
     * @return set of scrip IDs that should be excluded
     */
    public Set<String> findExcluded(UniverseDataFrame universe) {
        Set<String> excluded = new LinkedHashSet<>();

        for (String symbol : universe.getSymbols()) {
            Bars bars = universe.getBars(symbol);
            if (bars == null) {
                excluded.add(symbol);
                continue;
            }

            int size = bars.size();

            // Check minimum bar count
            if (size < minBars) {
                excluded.add(symbol);
                continue;
            }

            // Check zero-volume percentage
            int zeroVolCount = 0;
            for (int i = 0; i < size; i++) {
                if (bars.getVolume(i) == 0) {
                    zeroVolCount++;
                }
            }
            float zeroPct = (float) zeroVolCount / size;
            if (zeroPct > maxZeroVolumePct) {
                excluded.add(symbol);
            }
        }

        log.info("Data cleaning: excluded {} of {} symbols (minBars={}, maxZeroVolPct={}%)",
                excluded.size(), universe.getSymbols().length, minBars, (int) (maxZeroVolumePct * 100));

        return excluded;
    }
}

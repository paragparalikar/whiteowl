package com.whiteowl.core.backtest.v2.optimization;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.BarsArrays;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.bar.repository.BarsRepository;

import java.io.IOException;

/**
 * Loads bar data for one (scrip, timeframe) pair. Bars for a given pair are
 * loaded once per optimization run and reused across all parameter
 * combinations — implementations may cache.
 */
@FunctionalInterface
public interface BarsLoader {

    BarsArrays load(String scripId, Timeframe timeframe) throws IOException;

    /** Adapter over an existing {@link BarsRepository}. */
    static BarsLoader fromRepository(BarsRepository repository) {
        return (scripId, timeframe) -> {
            Bars bars = repository.load(scripId, timeframe);
            return bars == null ? null : bars.arrays();
        };
    }

}

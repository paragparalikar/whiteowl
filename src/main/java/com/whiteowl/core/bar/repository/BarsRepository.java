package com.whiteowl.core.bar.repository;

import com.whiteowl.core.bar.model.Bars;
import com.whiteowl.core.bar.model.Timeframe;

import java.io.IOException;
import java.util.Optional;

public interface BarsRepository {

    Bars load(String scripId, Timeframe timeframe) throws IOException;

    Bars loadRange(String scripId, Timeframe timeframe, int fromIndex, int toIndex) throws IOException;

    int countBars(String scripId, Timeframe timeframe) throws IOException;

    void save(String scripId, Timeframe timeframe, Bars bars) throws IOException;

    void append(String scripId, Timeframe timeframe, Bars bars, int fromIndex, int toIndex) throws IOException;

    Optional<Long> findLatestTimestamp(String scripId, Timeframe timeframe) throws IOException;

    Optional<Long> findEarliestTimestamp(String scripId, Timeframe timeframe) throws IOException;

    void prepend(String scripId, Timeframe timeframe, Bars bars, int fromIndex, int toIndex) throws IOException;

    boolean exists(String scripId, Timeframe timeframe);

    void delete(String scripId, Timeframe timeframe) throws IOException;

}

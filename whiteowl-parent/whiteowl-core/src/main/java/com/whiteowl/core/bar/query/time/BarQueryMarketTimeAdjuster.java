package com.whiteowl.core.bar.query.time;

import java.time.ZonedDateTime;
import java.util.function.BiFunction;

import com.whiteowl.core.bar.Timeframe;

public interface BarQueryMarketTimeAdjuster extends BiFunction<Timeframe, ZonedDateTime, ZonedDateTime> {

}

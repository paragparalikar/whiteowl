package com.whiteowl.core.bar.query.time;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.whiteowl.core.bar.Timeframe;

@Component
public class BarQueryFromAdjuster {

	private final BarQueryMarketTimeAdjuster marketTimeAdjuster = new BarQueryMarketFromAdjuster();
	private final List<BarQueryTimeAdjuster> adjusters = Arrays.asList(
			new FridayPostMarketFromAdjuster(),
			new SaturdayFromAdjuster(),
			new SundayFromAdjuster(),
			new WeekdayPostMarketFromAdjuster(),
			new WeekdayPreMarketFromAdjuster());
	
	public ZonedDateTime adjust(ZonedDateTime date, Timeframe timeframe) {
		if(Timeframe.D.equals(timeframe)) return date.truncatedTo(ChronoUnit.DAYS);
		for(BarQueryTimeAdjuster adjuster : adjusters) {
			if(adjuster.test(date)) {
				date = (ZonedDateTime) adjuster.adjustInto(date);
			}
		}
		return marketTimeAdjuster.apply(timeframe, date);
	}
}

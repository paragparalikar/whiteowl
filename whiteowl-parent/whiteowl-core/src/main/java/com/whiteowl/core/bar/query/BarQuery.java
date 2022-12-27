package com.whiteowl.core.bar.query;

import java.time.ZonedDateTime;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Scrip;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class BarQuery {

	@NonNull private final Scrip scrip;
	@NonNull private final ZonedDateTime to;
	@NonNull private final ZonedDateTime from;
	@NonNull private final Timeframe timeframe;
	
	public BarQuery withTo(@NonNull final ZonedDateTime to) {
		return BarQuery.builder()
				.scrip(scrip)
				.to(to)
				.from(from)
				.timeframe(timeframe)
				.build();
	}

	public BarQuery withFrom(@NonNull final ZonedDateTime from) {
		return BarQuery.builder()
				.scrip(scrip)
				.to(to)
				.from(from)
				.timeframe(timeframe)
				.build();
	}
	
}

package com.whiteowl.core.bar.query;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.whiteowl.core.bar.query.time.BarQueryFromAdjuster;
import com.whiteowl.core.bar.query.time.BarQueryToAdjuster;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BarQueryTransformer {

	private final BarQueryToAdjuster toAdjuster;
	private final BarQueryFromAdjuster fromAdjuster;
	private final BarQueryPredicate barQueryPredicate;
	
	public Optional<BarQuery> transform(@NonNull final BarQuery barQuery){
		final ZonedDateTime to = toAdjuster.adjust(barQuery.getTo(), barQuery.getTimeframe());
		final ZonedDateTime from = fromAdjuster.adjust(barQuery.getFrom(), barQuery.getTimeframe());
		final BarQuery transformedBarQuery = barQuery.withTo(to).withFrom(from);
		return Optional.of(transformedBarQuery).filter(barQueryPredicate);
	}
	
}

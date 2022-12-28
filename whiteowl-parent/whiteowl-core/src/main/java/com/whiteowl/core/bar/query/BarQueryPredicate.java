package com.whiteowl.core.bar.query;

import java.time.Duration;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import lombok.NonNull;

@Component
public class BarQueryPredicate implements Predicate<BarQuery> {

	@Override
	public boolean test(@NonNull final BarQuery barQuery) {
		final Duration timeframeDuration = barQuery.getTimeframe().getDuration();
		final Duration queryDuration = Duration.between(barQuery.getFrom(), barQuery.getTo());
		final Duration durationDelta = timeframeDuration.minus(queryDuration);
		return queryDuration.isZero() || 
				durationDelta.isZero() ||
				durationDelta.isNegative();
	}

}

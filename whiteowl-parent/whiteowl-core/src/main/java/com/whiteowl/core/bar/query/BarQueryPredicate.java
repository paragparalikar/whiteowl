package com.whiteowl.core.bar.query;

import java.time.Duration;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import lombok.NonNull;

@Component
public class BarQueryPredicate implements Predicate<BarQuery> {

	@Override
	public boolean test(@NonNull final BarQuery barQuery) {
		return 0 >= barQuery.getTimeframe().getDuration().compareTo(
				Duration.between(barQuery.getFrom(), barQuery.getTo()));
	}

}

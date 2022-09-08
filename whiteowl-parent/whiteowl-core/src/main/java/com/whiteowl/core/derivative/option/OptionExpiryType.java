package com.whiteowl.core.derivative.option;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.NonNull;

public enum OptionExpiryType {

	MONTHLY{
		@Override
		public Optional<LocalDate> resolve(final int index, @NonNull List<LocalDate> expiryDates) {
			final LocalDate now = LocalDate.now();
			return expiryDates.stream()
				.filter(date -> !date.isBefore(now))
				.filter(date -> date.plusWeeks(1).getMonthValue() > date.getMonthValue())
				.sorted()
				.skip(index)
				.findFirst();
		}
	}, 
	
	WEEKLY{
		@Override
		public Optional<LocalDate> resolve(final int index, @NonNull List<LocalDate> expiryDates) {
			final LocalDate now = LocalDate.now();
			return expiryDates.stream()
				.filter(date -> !date.isBefore(now))
				.sorted().skip(index).findFirst();
		}
	};
	
	public abstract Optional<LocalDate> resolve(int index, List<LocalDate> expiryDates);
	
}

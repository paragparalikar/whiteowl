package com.whiteowl.strategy;

import java.util.Optional;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.quote.Quote;

import lombok.NonNull;

public interface TradingStrategy {
	
	TradingStrategy NULL = new TradingStrategy() {
		@Override public Optional<Position> enter() { return Optional.empty(); }
		@Override public boolean manage(@NonNull Position position) { return false; }
		@Override public boolean manage(@NonNull Position position, @NonNull Quote quote) { return false; }
	};

	Optional<Position> enter();
	
	boolean manage(@NonNull final Position position);
	
	boolean manage(@NonNull final Position position, @NonNull final Quote quote);
	
}

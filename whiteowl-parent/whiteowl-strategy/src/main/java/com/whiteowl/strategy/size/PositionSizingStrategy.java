package com.whiteowl.strategy.size;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;

import lombok.NonNull;

public interface PositionSizingStrategy {

	void size(@NonNull final Position position, @NonNull final Portfolio portfolio);
	
}

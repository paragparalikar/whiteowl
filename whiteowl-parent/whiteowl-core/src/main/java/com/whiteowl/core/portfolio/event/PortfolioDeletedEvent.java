package com.whiteowl.core.portfolio.event;

import com.whiteowl.core.portfolio.Portfolio;

import lombok.NonNull;
import lombok.Value;

@Value
public class PortfolioDeletedEvent {

	@NonNull private final Portfolio portfolio;
	
}

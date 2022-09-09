package com.whiteowl.core.position;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PositionStatus {

	NEW(false), 
	OPEN(false), 
	CLOSED(true);
	
	private final boolean terminal;
	
}

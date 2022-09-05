package com.whiteowl.core.position;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PositionStatus {

	NEW(false), 
	OPENING(false), 
	OPEN(false), 
	CLOSING(false), 
	CLOSED(true);
	
	private final boolean terminal;
	
}

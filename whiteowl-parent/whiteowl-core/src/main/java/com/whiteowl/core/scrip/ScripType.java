package com.whiteowl.core.scrip;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScripType {

	EQ(false), FUT(true), CE(true), PE(true);
	
	private final boolean derivative;
}

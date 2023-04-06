package com.whiteowl.core.scrip;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScripType {

	EQ(false, false), FUT(true, false), CE(true, true), PE(true, true);
	
	private final boolean derivative, option;
}

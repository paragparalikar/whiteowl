package com.whiteowl.core.scrip;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Index {

	NIFTY50("Nifty 50"),
	NIFTYNEXT50("Nifty Next 50"),
	NIFTY100("Nifty 100"),
	NIFTY200("Nifty 200"), 
	NIFTY500("Nifty 500");
	
	private final String name;
	
}

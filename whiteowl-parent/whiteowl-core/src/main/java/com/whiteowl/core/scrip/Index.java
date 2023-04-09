package com.whiteowl.core.scrip;

import java.util.Arrays;
import java.util.function.Predicate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Index {

	VIX("INDIA VIX", "India VIX"),
	NIFTYBANK("NIFTY BANK", "Nifty Bank"),
	NIFTYFINSERVICE("NIFTY FIN SERVICE", "Nifty Financial Services"),
	NIFTYMIDCAP("NIFTY MID SELECT", "Nifty Midcap"),
	NIFTY50("NIFTY 50", "Nifty 50"),
	NIFTYNEXT50("NIFTY NEXT 50", "Nifty Next 50"),
	NIFTY100("NIFTY 100", "Nifty 100"),
	NIFTY200("NIFTY 200", "Nifty 200"), 
	NIFTY500("NIFTY 500", "Nifty 500");
	
	private final String code, displayName;
	
	public static boolean isIndex(String code) {
		return Arrays.stream(Index.values())
				.map(Index::getCode)
				.anyMatch(Predicate.isEqual(code));
	}
	
}

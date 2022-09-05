package com.whiteowl.core.scrip;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Index {

	NIFTY50("Nifty 50", "https://www1.nseindia.com/content/indices/ind_nifty50list.csv"),
	NIFTYNEXT50("Nifty Next 50", "https://www1.nseindia.com/content/indices/ind_niftynext50list.csv"),
	NIFTY100("Nifty 100", "https://www1.nseindia.com/content/indices/ind_nifty100list.csv"),
	NIFTY200("Nifty 200", "https://www1.nseindia.com/content/indices/ind_nifty200list.csv"), 
	NIFTY500("Nifty 500", "https://www1.nseindia.com/content/indices/ind_nifty500list.csv");
	
	private final String name;
	private final String url;
	
}

package com.whiteowl.core.quote;

import java.util.Collection;

import com.whiteowl.core.scrip.Scrip;

public interface QuoteDataProvider {
	
	Collection<Quote> getQuotes(Collection<Scrip> scrips, QuoteMode mode);
	
}

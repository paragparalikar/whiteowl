package com.whiteowl.core.quote;

import com.whiteowl.core.scrip.Scrip;

public interface QuoteService {

	Quote getQuote(Scrip scrip, QuoteMode mode);
	
}

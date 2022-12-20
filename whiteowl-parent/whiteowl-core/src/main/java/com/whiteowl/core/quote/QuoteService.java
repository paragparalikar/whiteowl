package com.whiteowl.core.quote;

import com.whiteowl.core.scrip.Scrip;

public interface QuoteService {
	
	QuoteSubscription subscribe(Scrip scrip, QuoteMode mode);

	Quote getQuote(Scrip scrip, QuoteMode mode);

	Quote getFullQuote(Scrip scrip);

	Quote getOhlcQuote(Scrip scrip);

	Quote getLtpQuote(Scrip scrip);
	
}

package com.whiteowl.core.quote;

import java.util.Collection;
import java.util.function.Consumer;

import com.whiteowl.core.scrip.Scrip;

public interface QuoteService {
	
	Collection<Quote> getQuotes(Collection<Scrip> scrips, QuoteMode mode);
	
	void subscribe(Collection<Scrip> scrips, QuoteMode mode, Consumer<Quote> quoteListener);
	
	void unsubscribe(Consumer<Quote> quoteListener);

}

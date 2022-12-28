package com.whiteowl.strategy.test.mock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.quote.QuoteSubscription;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;

public class MockQuoteService implements QuoteService {

	private final Map<Scrip, QuoteSubscription> subscriptions = new ConcurrentHashMap<>();
	
	@Override
	public QuoteSubscription subscribe(@NonNull final Scrip scrip) {
		return subscribe(scrip, QuoteMode.LTP);
	}

	@Override
	public QuoteSubscription subscribe(@NonNull final Scrip scrip, @NonNull final QuoteMode mode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Quote getQuote(@NonNull final Scrip scrip, @NonNull final QuoteMode mode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Quote getFullQuote(@NonNull final Scrip scrip) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Quote getOhlcQuote(@NonNull final Scrip scrip) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Quote getLtpQuote(@NonNull final Scrip scrip) {
		throw new UnsupportedOperationException();
	}

}

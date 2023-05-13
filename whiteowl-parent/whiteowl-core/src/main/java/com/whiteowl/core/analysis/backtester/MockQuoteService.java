package com.whiteowl.core.analysis.backtester;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.scrip.Scrip;

public class MockQuoteService implements QuoteService {
	
	private final Map<String, Quote> quotes = new ConcurrentHashMap<>();
	private final Map<String, List<Consumer<Quote>>> consumers = new ConcurrentHashMap<>();
	
	public void push(String code, Quote quote) {
		quotes.put(code, quote);
		final List<Consumer<Quote>> consumers = this.consumers.get(code);
		if(null != consumers && !consumers.isEmpty()) {
			for(int index = 0; index < consumers.size(); index++) {
				final Consumer<Quote> consumer = consumers.get(index);
				consumer.accept(quote);
			}
		}
	}

	@Override
	public Collection<Quote> getQuotes(Collection<Scrip> scrips, QuoteMode mode) {
		return scrips.stream()
				.map(Scrip::getCode)
				.map(quotes::get)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	@Override
	public void subscribe(Collection<Scrip> scrips, QuoteMode mode, Consumer<Quote> quoteListener) {
		for(Scrip scrip : scrips) {
			consumers.computeIfAbsent(scrip.getCode(), key -> new ArrayList<>()).add(quoteListener);
		}
	}

	@Override
	public void unsubscribe(Consumer<Quote> quoteListener) {
		consumers.values().forEach(consumers -> consumers.remove(quoteListener));
	}

}

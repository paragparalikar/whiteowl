package com.whiteowl.core.strategy.backtester;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.scrip.Scrip;

public class MockQuoteService implements QuoteService {
	
	private final Map<Scrip, Set<Quote>> quotes = new ConcurrentHashMap<>();
	private final Map<Scrip, Set<Consumer<Quote>>> consumers = new ConcurrentHashMap<>();
	
	public void clear() {
		quotes.clear();
	}
	
	public void push(Scrip scrip, List<Quote> quotes) {
		quotes.forEach(quote -> this.quotes.computeIfAbsent(scrip, key -> Collections.newSetFromMap(new IdentityHashMap<>())).add(quote));
		Optional.ofNullable(consumers.get(scrip)).ifPresent(consumers -> {
			for(Consumer<Quote> consumer : consumers) {
				for(Quote quote : quotes) {
					consumer.accept(quote);
				}
			}
		});
	}

	@Override
	public Collection<Quote> getQuotes(Collection<Scrip> scrips, QuoteMode mode) {
		return scrips.stream()
				.map(quotes::get)
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
	}

	@Override
	public void subscribe(Collection<Scrip> scrips, QuoteMode mode, Consumer<Quote> quoteListener) {
		for(Scrip scrip : scrips) {
			consumers.computeIfAbsent(scrip, key -> Collections.newSetFromMap(new IdentityHashMap<>()))
				.add(quoteListener);
		}
	}

	@Override
	public void unsubscribe(Consumer<Quote> quoteListener) {
		consumers.values().forEach(consumers -> consumers.remove(quoteListener));
	}

}

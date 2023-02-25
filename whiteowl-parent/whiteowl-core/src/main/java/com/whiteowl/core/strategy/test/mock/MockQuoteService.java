package com.whiteowl.core.strategy.test.mock;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;
import org.ta4j.core.num.Num;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;

public class MockQuoteService implements QuoteService {

	private final Map<Scrip, Set<Consumer<Quote>>> quoteListeners = new ConcurrentHashMap<>();
	
	@Override
	public Collection<Quote> getQuotes(Collection<Scrip> scrips, QuoteMode mode) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void subscribe(Collection<Scrip> scrips, QuoteMode mode, Consumer<Quote> quoteListener) {
		if(!QuoteMode.LTP.equals(mode)) throw new IllegalArgumentException();
		for(Scrip scrip : scrips) {
			quoteListeners.computeIfAbsent(scrip, key -> new HashSet<>()).add(quoteListener);
		}
	}
	
	@Override
	public void unsubscribe(Consumer<Quote> quoteListener) {
		quoteListeners.values().forEach(quoteListeners -> quoteListeners.remove(quoteListener));
	}
	
	public void publish(
			@NonNull final Bar bar,
			@NonNull final Scrip scrip, 
			@NonNull final TradeType tradeType) {
		final Set<Consumer<Quote>> quoteListeners = this.quoteListeners.get(scrip);
		if(null != quoteListeners && !quoteListeners.isEmpty()) {
			Stream.of(bar.getOpenPrice(),
					TradeType.BUY.equals(tradeType) ? bar.getLowPrice() : bar.getHighPrice(),
					TradeType.BUY.equals(tradeType) ? bar.getHighPrice() : bar.getLowPrice(),
					bar.getClosePrice())
			.map(Num::doubleValue)
			.map(value -> Quote.builder().code(scrip.getCode()).lastPrice(value).build())
			.forEach(quote -> quoteListeners.forEach(quoteListener -> quoteListener.accept(quote)));
		}
	}

	
}

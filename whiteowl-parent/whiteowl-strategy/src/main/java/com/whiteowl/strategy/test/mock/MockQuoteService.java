package com.whiteowl.strategy.test.mock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;
import org.ta4j.core.num.Num;

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
		if(!QuoteMode.LTP.equals(mode)) throw new IllegalArgumentException();
		final QuoteSubscription subscription = QuoteSubscription.builder()
				.scrip(scrip)
				.mode(mode)
				.unsubscribeCallback(sub -> subscriptions.remove(scrip))
				.build();
		subscriptions.put(scrip, subscription);
		return subscription;
	}
	
	public void publish(
			@NonNull final Bar bar,
			@NonNull final Scrip scrip, 
			@NonNull final TradeType tradeType) {
		final QuoteSubscription subscription = subscriptions.get(scrip);
		if(null != subscription) {
			Stream.of(bar.getOpenPrice(),
					TradeType.BUY.equals(tradeType) ? bar.getLowPrice() : bar.getHighPrice(),
					TradeType.BUY.equals(tradeType) ? bar.getHighPrice() : bar.getLowPrice(),
					bar.getClosePrice())
			.map(Num::doubleValue)
			.map(value -> Quote.builder().code(scrip.getCode()).lastPrice(value).build())
			.forEach(subscription::onQuote);
		}
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

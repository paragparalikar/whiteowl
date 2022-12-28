package com.whiteowl.strategy.test.mock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.ta4j.core.Bar;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.quote.QuoteSubscription;
import com.whiteowl.core.scrip.Scrip;

import lombok.NonNull;

public class MockQuoteService implements QuoteService {
	private static final Num TWO = DoubleNum.valueOf(2);

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
	
	public void publish(@NonNull final Scrip scrip, @NonNull final Bar bar) {
		final QuoteSubscription subscription = subscriptions.get(scrip);
		if(null != subscription) {
			Stream.of(bar.getOpenPrice(),
					bar.getOpenPrice().plus(bar.getHighPrice()).dividedBy(TWO),
					bar.getHighPrice(),
					bar.getHighPrice().plus(bar.getLowPrice()).dividedBy(TWO),
					bar.getLowPrice(),
					bar.getLowPrice().plus(bar.getClosePrice()).dividedBy(TWO),
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

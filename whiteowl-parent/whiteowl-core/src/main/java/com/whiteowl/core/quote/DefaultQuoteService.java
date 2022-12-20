package com.whiteowl.core.quote;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.util.Tuple2;

@Service
public class DefaultQuoteService implements QuoteService {

	@Autowired
	private QuoteDataProvider quoteDataProvider;
	
	@Value("${whiteowl.quote.ttl:15s}")
	private Duration quoteTtl;
	
	private final Map<Tuple2<Scrip, QuoteMode>, Tuple2<Quote, LocalDateTime>> cache = new HashMap<>();
	
	@Override
	public Quote getQuote(Scrip scrip, QuoteMode mode) {
		switch(mode) {
			case LTP: return getLtpQuote(scrip);
			case OHLC: return getOhlcQuote(scrip);
			case FULL: return getFullQuote(scrip);
			default: throw new IllegalArgumentException(String.format("QuoteMode %s is not supported", mode.name()));
		}
	}
	
	public Quote getLtpQuote(Scrip scrip) {
		final Tuple2<Scrip, QuoteMode> key = Tuple2.of(scrip, QuoteMode.LTP);
		final Tuple2<Quote, LocalDateTime> value = cache.get(key);
		if(null != value && !isExpired(value)) {
			return value.getKey();
		} else {
			final Tuple2<Quote, LocalDateTime> ohlcQuoteValue = cache.get(Tuple2.of(scrip, QuoteMode.OHLC));
			if(null != ohlcQuoteValue && !isExpired(ohlcQuoteValue)) {
				return ohlcQuoteValue.getKey();
			} else {
				final Tuple2<Quote, LocalDateTime> fullQuoteValue = cache.get(Tuple2.of(scrip, QuoteMode.FULL));
				if(null != fullQuoteValue && !isExpired(fullQuoteValue)) {
					return fullQuoteValue.getKey();
				} else {
					final Quote quote = quoteDataProvider.getQuotes(Collections.singleton(scrip), QuoteMode.LTP).stream()
							.findFirst().orElse(null);
					cache.put(key, Tuple2.of(quote, LocalDateTime.now()));
					return quote;
				}
			}
		}
	}
	
	public Quote getOhlcQuote(Scrip scrip) {
		final Tuple2<Scrip, QuoteMode> key = Tuple2.of(scrip, QuoteMode.OHLC);
		final Tuple2<Quote, LocalDateTime> value = cache.get(key);
		if(null != value && !isExpired(value)) {
			return value.getKey();
		} else {
			final Tuple2<Quote, LocalDateTime> fullQuoteValue = cache.get(Tuple2.of(scrip, QuoteMode.FULL));
			if(null != fullQuoteValue && !isExpired(fullQuoteValue)) {
				return fullQuoteValue.getKey();
			} else {
				final Quote quote = quoteDataProvider.getQuotes(Collections.singleton(scrip), QuoteMode.OHLC).stream()
						.findFirst().orElse(null);
				cache.put(key, Tuple2.of(quote, LocalDateTime.now()));
				return quote;
			}
		}
	}
	
	public Quote getFullQuote(Scrip scrip) {
		final Tuple2<Scrip, QuoteMode> key = Tuple2.of(scrip, QuoteMode.FULL);
		final Tuple2<Quote, LocalDateTime> value = cache.get(key);
		if(null == value || isExpired(value)) {
			final Quote quote = quoteDataProvider.getQuotes(Collections.singleton(scrip), QuoteMode.FULL).stream()
				.findFirst().orElse(null);
			cache.put(key, Tuple2.of(quote, LocalDateTime.now()));
			return quote;
		} 
		return value.getKey();
	}
	
	private boolean isExpired(Tuple2<Quote, LocalDateTime> value) {
		return LocalDateTime.now().minus(quoteTtl).isAfter(value.getValue());
	}

}

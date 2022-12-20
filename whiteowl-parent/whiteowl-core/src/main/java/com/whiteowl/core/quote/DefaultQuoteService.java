package com.whiteowl.core.quote;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.util.Tuple2;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DefaultQuoteService implements QuoteService {
	
	@Autowired private QuoteDataProvider quoteDataProvider;
	@Value("${whiteowl.quote.ttl:15s}") private Duration quoteTtl;
	private final Map<Tuple2<Scrip, QuoteMode>, Tuple2<Quote, LocalDateTime>> quotesCache = new HashMap<>(); // TODO use hazelcast
	private final Map<Tuple2<Scrip, QuoteMode>, Set<QuoteSubscription>> subscriptionsCache = new ConcurrentHashMap<>();
	
	@Scheduled(initialDelayString = "15s", fixedRateString = "${whiteowl.quote.ttl:15s}")
	public void poll() {
		for(QuoteMode mode : Arrays.asList(QuoteMode.FULL, QuoteMode.OHLC, QuoteMode.LTP)) {
			subscriptionsCache.entrySet().stream()
				.filter(entry -> entry.getKey().getValue().equals(mode))
				.forEach(entry -> poll(entry.getKey().getKey(), entry.getKey().getValue(), entry.getValue()));
		}
	}
	
	private void poll(Scrip scrip, QuoteMode mode, Set<QuoteSubscription> subscriptions) {
		final Quote quote = getQuote(scrip, mode);
		if(null != quote) {
			for(QuoteSubscription subscription : subscriptions) {
				try {
					subscription.onQuote(quote);
				} catch(Exception e) {
					log.error("", e);
				}
			}
		};
	}
	
	@Override
	public QuoteSubscription subscribe(@NonNull final Scrip scrip, @NonNull final QuoteMode mode) {
		final QuoteSubscription subscription = QuoteSubscription.builder()
				.scrip(scrip)
				.mode(mode)
				.unsubscribeCallback(this::unsubscribe)
				.build();
		subscriptionsCache.computeIfAbsent(
				Tuple2.of(scrip, mode), 
				key -> Collections.newSetFromMap(new IdentityHashMap<>()))
			.add(subscription);
		return subscription;
	}
	
	public void unsubscribe(QuoteSubscription quoteSubscription) {
		subscriptionsCache.values().forEach(subscriptions -> subscriptions.remove(quoteSubscription));
		final Iterator<Entry<Tuple2<Scrip, QuoteMode>, Set<QuoteSubscription>>> iterator = 
				subscriptionsCache.entrySet().iterator();
		while(iterator.hasNext()) {
			final Entry<Tuple2<Scrip, QuoteMode>, Set<QuoteSubscription>> entry = iterator.next();
			final Set<QuoteSubscription> subscriptions = entry.getValue();
			if(null == subscriptions || subscriptions.isEmpty()) iterator.remove();
		}
	}
	
	@Override
	public Quote getQuote(@NonNull final Scrip scrip, @NonNull final QuoteMode mode) {
		switch(mode) {
			case LTP: return getLtpQuote(scrip);
			case OHLC: return getOhlcQuote(scrip);
			case FULL: return getFullQuote(scrip);
			default: throw new IllegalArgumentException(String.format("QuoteMode %s is not supported", mode.name()));
		}
	}
	
	@Override
	public Quote getLtpQuote(@NonNull final Scrip scrip) {
		final Tuple2<Scrip, QuoteMode> key = Tuple2.of(scrip, QuoteMode.LTP);
		final Tuple2<Quote, LocalDateTime> value = quotesCache.get(key);
		if(null != value && !isExpired(value)) {
			return value.getKey();
		} else {
			final Tuple2<Quote, LocalDateTime> ohlcQuoteValue = quotesCache.get(Tuple2.of(scrip, QuoteMode.OHLC));
			if(null != ohlcQuoteValue && !isExpired(ohlcQuoteValue)) {
				return ohlcQuoteValue.getKey();
			} else {
				final Tuple2<Quote, LocalDateTime> fullQuoteValue = quotesCache.get(Tuple2.of(scrip, QuoteMode.FULL));
				if(null != fullQuoteValue && !isExpired(fullQuoteValue)) {
					return fullQuoteValue.getKey();
				} else {
					final Quote quote = quoteDataProvider.getQuotes(Collections.singleton(scrip), QuoteMode.LTP).stream()
							.findFirst().orElse(null);
					quotesCache.put(key, Tuple2.of(quote, LocalDateTime.now()));
					return quote;
				}
			}
		}
	}
	
	@Override
	public Quote getOhlcQuote(@NonNull final Scrip scrip) {
		final Tuple2<Scrip, QuoteMode> key = Tuple2.of(scrip, QuoteMode.OHLC);
		final Tuple2<Quote, LocalDateTime> value = quotesCache.get(key);
		if(null != value && !isExpired(value)) {
			return value.getKey();
		} else {
			final Tuple2<Quote, LocalDateTime> fullQuoteValue = quotesCache.get(Tuple2.of(scrip, QuoteMode.FULL));
			if(null != fullQuoteValue && !isExpired(fullQuoteValue)) {
				return fullQuoteValue.getKey();
			} else {
				final Quote quote = quoteDataProvider.getQuotes(Collections.singleton(scrip), QuoteMode.OHLC).stream()
						.findFirst().orElse(null);
				quotesCache.put(key, Tuple2.of(quote, LocalDateTime.now()));
				return quote;
			}
		}
	}
	
	@Override
	public Quote getFullQuote(@NonNull final Scrip scrip) {
		final Tuple2<Scrip, QuoteMode> key = Tuple2.of(scrip, QuoteMode.FULL);
		final Tuple2<Quote, LocalDateTime> value = quotesCache.get(key);
		if(null == value || isExpired(value)) {
			final Quote quote = quoteDataProvider.getQuotes(Collections.singleton(scrip), QuoteMode.FULL).stream()
				.findFirst().orElse(null);
			quotesCache.put(key, Tuple2.of(quote, LocalDateTime.now()));
			return quote;
		} 
		return value.getKey();
	}
	
	private boolean isExpired(Tuple2<Quote, LocalDateTime> value) {
		return LocalDateTime.now().minus(quoteTtl).isAfter(value.getValue());
	}

}

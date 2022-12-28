package com.whiteowl.core.quote;

import java.time.Duration;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.util.ConcurrentExpiryMap;
import com.whiteowl.core.util.Tuple2;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DefaultQuoteService implements QuoteService {
	
	@Autowired private QuoteDataProvider quoteDataProvider;
	@Value("${whiteowl.quote.ttl:15s}") private Duration quoteTtl;
	private final ConcurrentExpiryMap<Tuple2<Scrip, QuoteMode>, Quote> quotesCache = new ConcurrentExpiryMap<>(); // TODO use hazelcast
	private final Map<Tuple2<Scrip, QuoteMode>, Set<QuoteSubscription>> subscriptionsCache = new ConcurrentHashMap<>();
	
	@PostConstruct
	public void init() {
		quotesCache.setTtlInMillis(quoteTtl.toMillis());
	}
	
	public void poll() {
		for(QuoteMode mode : QuoteMode.values()) {
			final Map<String, Scrip> scripCodeMapping = subscriptionsCache.keySet().stream()
					.filter(tuple -> mode.equals(tuple.getValue()))
					.<Scrip>map(Tuple2::getKey)
					.collect(Collectors.toMap(Scrip::getCode, Function.identity()));
			for(Quote quote : quoteDataProvider.getQuotes(scripCodeMapping.values(), mode)) {
				if(null != quote) {
					final Scrip scrip = scripCodeMapping.get(quote.getCode());
					onQuote(scrip, mode, quote);
				}
			}
		}
	}
	
	private void onQuote(Scrip scrip, QuoteMode mode, Quote quote) {
		final Tuple2<Scrip, QuoteMode> key = Tuple2.of(scrip, mode); 
		quotesCache.put(key, quote);
		for(QuoteSubscription subscription : subscriptionsCache.get(key)) {
			try {
				subscription.onQuote(quote);
			} catch(Exception e) {
				log.error("", e);
			}
		}
	}
	
	@Override
	public QuoteSubscription subscribe(Scrip scrip) {
		return subscribe(scrip, QuoteMode.LTP);
	}
	
	@Override
	public QuoteSubscription subscribe(@NonNull final Scrip scrip, @NonNull final QuoteMode mode) {
		final QuoteSubscription subscription = new QuoteSubscription(scrip, mode, this::unsubscribe);
		subscriptionsCache.computeIfAbsent(Tuple2.of(scrip, mode), 
				key -> Collections.newSetFromMap(new IdentityHashMap<>())).add(subscription);
		return subscription;
	}
	
	void unsubscribe(@NonNull QuoteSubscription quoteSubscription) {
		final Tuple2<Scrip, QuoteMode> key = Tuple2.of(quoteSubscription.getScrip(), quoteSubscription.getMode());
		final Set<QuoteSubscription> values = subscriptionsCache.getOrDefault(key, Collections.emptySet());
		values.remove(quoteSubscription);
		if(values.isEmpty()) subscriptionsCache.remove(key);
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
		return get(scrip, QuoteMode.LTP, QuoteMode.OHLC, QuoteMode.FULL);
	}
	
	@Override
	public Quote getOhlcQuote(@NonNull final Scrip scrip) {
		return get(scrip, QuoteMode.OHLC, QuoteMode.FULL);
	}
	
	@Override
	public Quote getFullQuote(@NonNull final Scrip scrip) {
		return get(scrip, QuoteMode.FULL);
	}
	
	private Quote get(@NonNull final Scrip scrip, @NonNull QuoteMode...modes) {
		Quote quote = getCachedQuote(scrip, modes);
		if(null == quote) {
			final QuoteMode mode = modes[0];
			quote = quoteDataProvider.getQuotes(Collections.singleton(scrip), mode).stream()
					.findFirst().orElse(null);
			quotesCache.put(Tuple2.of(scrip, mode), quote);
		}
		return quote;
	}

	private Quote getCachedQuote(@NonNull final Scrip scrip, QuoteMode...modes) {
		for(QuoteMode mode : modes) {
			final Quote quote = quotesCache.get(Tuple2.of(scrip, mode));
			if(null != quote) return quote;
		}
		return null;
	}
	
}

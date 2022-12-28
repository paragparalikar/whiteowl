package com.whiteowl.core.quote;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.util.Strings;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class QuoteSubscription {

	@NonNull private final Scrip scrip;
	@NonNull private final QuoteMode mode;
	@NonNull private final Consumer<QuoteSubscription> unsubscribeCallback;
	private final Set<Consumer<Quote>> listeners = Collections.newSetFromMap(new IdentityHashMap<>());
	
	public QuoteSubscription unsubscribe() {
		unsubscribeCallback.accept(this);
		return this;
	}
	
	public QuoteSubscription addListener(@NonNull final Consumer<Quote> listener) {
		listeners.add(listener);
		return this;
	}
	
	public QuoteSubscription removeListener(@NonNull final Consumer<Quote> listener) {
		listeners.remove(listener);
		return this;
	}
	
	public void onQuote(@NonNull final Quote quote) {
		if(Strings.hasText(quote.getCode()) && quote.getCode().equalsIgnoreCase(scrip.getCode())) {
			for(Consumer<Quote> listener : listeners) {
				try {
					listener.accept(quote);
				} catch(Exception e) {
					log.error("", e);
				}
			}
		}
	}

}

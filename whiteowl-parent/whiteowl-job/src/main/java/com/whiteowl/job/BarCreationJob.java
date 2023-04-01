package com.whiteowl.job;

import static com.whiteowl.core.util.Constant.NSE_START_TIME;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DoubleNum;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.bar.event.BarCreatedEvent;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripCriteria;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.core.util.Dates;
import com.whiteowl.core.util.Tuple2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Component
@RequiredArgsConstructor
@Order(JobConstant.ORDER_BAR_CREATION)
public class BarCreationJob implements CommandLineRunner, Consumer<Quote>, AutoCloseable {
	
	private final BarService barService;
	private final ScripService scripService;
	private final QuoteService quoteService;
	private final ApplicationEventPublisher eventPublisher;
	private final Map<String, Quote> lastQuotes = new ConcurrentHashMap<>();
	private final Map<Tuple2<String, Timeframe>, Bar> cache = new ConcurrentHashMap<>();

	@Override
	public void run(String... args) throws Exception {
		final Set<Scrip> scrips = scripService.findAll().stream()
			.filter(ScripCriteria.UNDERLYINGS)
			.collect(Collectors.toSet());
		quoteService.subscribe(scrips, QuoteMode.FULL, this);
		log.info("Subscribed to {} scrip quotes", scrips.size());
	}
	
	@Override
	public void accept(final Quote quote) {
		if(log.isTraceEnabled()) log.trace("Received quote : {}", quote);
		final String scripCode = quote.getCode();
		synchronized(scripCode) {
			final Quote lastQuote = lastQuotes.get(scripCode);
			lastQuotes.put(scripCode, quote);
			for(Timeframe timeframe : Timeframe.values()) {
				final Duration duration = timeframe.getDuration();
				final Tuple2<String, Timeframe> tuple2 = Tuple2.of(scripCode, timeframe);
				final ZonedDateTime endTime = Dates.truncate(quote.getTimestamp(), NSE_START_TIME, duration).plus(duration);
				final Bar bar = cache.compute(tuple2, (key, value) -> {
					if(null == value) {
						return new BaseBar(duration, endTime, DoubleNum::valueOf);
					} else if(Objects.equals(endTime, value.getEndTime())) {
						return value;
					} else {
						if(log.isTraceEnabled()) log.trace("Created new bar for {} - {} - {}", scripCode, timeframe, value);
						final List<Bar> bars = Collections.singletonList(value);
						barService.saveAll(scripCode, timeframe, bars);
						final Scrip scrip = scripService.findByCode(scripCode);
						eventPublisher.publishEvent(new BarCreatedEvent(value, scrip, timeframe));
						return new BaseBar(duration, endTime, DoubleNum::valueOf);
					}
				});
				final long volume = quote.getVolume() - Optional.ofNullable(lastQuote).map(Quote::getVolume).orElse(0L);
				bar.addTrade(DoubleNum.valueOf(volume), DoubleNum.valueOf(quote.getLastPrice()));
			}
		}
	}
	
	private void flush() {
		for(String scripCode : lastQuotes.keySet()) {
			final Quote lastQuote = lastQuotes.get(scripCode);
			for(Timeframe timeframe : Timeframe.values()) {
				Optional.ofNullable(cache.get(Tuple2.of(scripCode, timeframe))).ifPresent(bar -> {
					final ZonedDateTime quoteTime = ZonedDateTime.of(lastQuote.getTimestamp(), ZoneId.systemDefault());
					if(quoteTime.isAfter(bar.getEndTime().minusSeconds(3))) {
						barService.saveAll(scripCode, timeframe, Collections.singleton(bar));
					}
				});
			}
		}
	}
	
	@Override
	public void close() throws Exception {
		quoteService.unsubscribe(this);
		flush();
		cache.clear();
		lastQuotes.clear();
	}

}

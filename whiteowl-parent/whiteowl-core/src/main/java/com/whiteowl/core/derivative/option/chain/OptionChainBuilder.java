package com.whiteowl.core.derivative.option.chain;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.core.util.Tuple2;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OptionChainBuilder {
	private static final Timeframe TIMEFRAME = Timeframe.M3;

	private final BarService barService;
	private final ScripService scripService;
	
	public OptionChain build(Scrip underlying, LocalDate expiry, Bar bar) {
		final ZonedDateTime timestamp = bar.getBeginTime();
		final Set<Scrip> optionScrips = findOptionScrips(underlying.getCode(), expiry, bar.getClosePrice().doubleValue());
 		final Set<Option> options = optionScrips.stream()
 			.map(scrip -> Tuple2.of(scrip, barService.findByCodeAndTimeframeAndBeginTime(
				scrip.getCode(), TIMEFRAME, timestamp)))
 			.map(tuple2 -> buildOption(tuple2.getKey(), tuple2.getValue()))
 			.filter(Objects::nonNull)
 			.collect(Collectors.toSet());
		return OptionChain.builder()
				.expiry(expiry)
				.options(options)
				.timestamp(timestamp)
				.underlying(underlying)
				.spotPrice(bar.getClosePrice().doubleValue())
				.build();
	}

	public OptionChain build(Scrip underlying, LocalDate expiry, ZonedDateTime timestamp) {
		final Bar bar = barService.findByCodeAndTimeframeAndBeginTime(underlying.getCode(), TIMEFRAME, timestamp);
		if(null == bar) return null;
		return build(underlying, expiry, bar);
	}
	
	private Option buildOption(Scrip scrip, Bar bar) {
		return null == bar ? null : Option.builder()
				.code(scrip.getCode())
				.type(scrip.getType())
				.strike(scrip.getStrike())
				.volume(bar.getVolume().doubleValue())
				.ltp(bar.getClosePrice().doubleValue())
				.oi(bar.getOpenInterest().doubleValue())
				.build();
	}
	
	private Set<Scrip> findOptionScrips(String underlyingCode, LocalDate expiry, double spotPrice){
		return scripService.findAll().stream()
				.filter(scrip -> scrip.isOption())
				.filter(scrip -> null != scrip.getExpiry())
				.filter(scrip -> expiry.isEqual(scrip.getExpiry()))
				.filter(scrip -> underlyingCode.equalsIgnoreCase(resolveUnderlyingCode(scrip)))
				.sorted(Comparator.comparing(scrip -> Math.abs(spotPrice - scrip.getStrike())))
				.limit(40)
				.collect(Collectors.toSet());
	}
	
	private String resolveUnderlyingCode(Scrip scrip) {
		final String name = scrip.getName();
		if("NIFTY".equalsIgnoreCase(name)) return Index.NIFTY50.getCode();
		if("BANKNIFTY".equalsIgnoreCase(name)) return Index.NIFTYBANK.getCode();
		return name;
	}

}

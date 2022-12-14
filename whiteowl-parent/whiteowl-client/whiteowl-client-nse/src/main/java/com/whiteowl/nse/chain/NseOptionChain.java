package com.whiteowl.nse.chain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.scrip.Scrip;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NseOptionChain {

	@JsonFormat(shape = Shape.STRING, pattern = "dd-MMM-yyyy")
	private final List<LocalDate> expiryDates = new ArrayList<>();

	private final List<Double> strikePrices = new ArrayList<>();
	
	private final List<NseOptionChainItem> data = new ArrayList<>();
	
	@JsonFormat(shape = Shape.STRING, pattern = "dd-MMM-yyyy HH:mm:ss")
	private LocalDateTime timestamp;
	
	private double underlyingValue;
	
	private NseIndexInfo index;
	
	public OptionChain toOptionChain(Scrip underlying, Function<String, Scrip> scripResolver) {
		final OptionChain optionChain = OptionChain.builder()
				.indexInfo(Optional.ofNullable(index).map(NseIndexInfo::toIndexInfo).orElse(null))
				.underlying(underlying)
				.underlyingValue(underlyingValue)
				.timestamp(timestamp)
				.build();
		optionChain.getStrikePrices().addAll(strikePrices);
		optionChain.getExpiryDates().addAll(expiryDates);
		data.stream()
			.map(item -> item.toOptionChainItem(underlying, scripResolver))
			.forEach(optionChain.getItems()::add);
		return optionChain;
	}

}

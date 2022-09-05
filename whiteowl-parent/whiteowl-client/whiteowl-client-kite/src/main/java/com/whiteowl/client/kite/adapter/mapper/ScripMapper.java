package com.whiteowl.client.kite.adapter.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.util.Strings;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ScripMapper {
	
	private final ExchangeMapper exchangeMapper;
	private final ScripTypeMapper scripTypeMapper;
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public Scrip toScrip(Instrument instrument) {
		return Scrip.builder()
				.code(instrument.getTradingsymbol())
				.name(Optional.ofNullable(instrument.getName()).map(name -> name.replaceAll("\"", "")).orElse(null))
				.expiry(Strings.hasText(instrument.getExpiry()) ? LocalDate.parse(instrument.getExpiry(), formatter) : null)
				.strike(instrument.getStrike())
				.tickSize(instrument.getTickSize())
				.lotSize(instrument.getLotSize())
				.type(scripTypeMapper.toScripType(instrument.getInstrumentType()))
				.segment(instrument.getSegment())
				.exchange(exchangeMapper.toExchange(instrument.getExchange()))
				.build();
	}
	
}

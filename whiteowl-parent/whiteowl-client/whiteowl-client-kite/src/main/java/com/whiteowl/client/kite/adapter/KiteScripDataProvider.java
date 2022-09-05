package com.whiteowl.client.kite.adapter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.whiteowl.client.kite.adapter.mapper.KiteMapper;
import com.whiteowl.core.scrip.IndexPopulator;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripDataProvider;

import lombok.RequiredArgsConstructor;

@Primary
@Component
@RequiredArgsConstructor
public class KiteScripDataProvider implements ScripDataProvider {
	
	private final KiteMapper kiteMapper;
	private final IndexPopulator indexPopulator;
	private final KiteInstrumentService kiteInstrumentService;
	
	@Override
	public List<Scrip> getAllScrips() {
		final List<Scrip> scrips = kiteInstrumentService.getAllInstruments().stream()
				.map(kiteMapper::toScrip)
				.collect(Collectors.toList());
		indexPopulator.populate(scrips);
		return scrips;
	}

}

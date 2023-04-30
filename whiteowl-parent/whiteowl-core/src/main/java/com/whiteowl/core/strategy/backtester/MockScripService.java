package com.whiteowl.core.strategy.backtester;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;

public class MockScripService implements ScripService {
	
	private List<Scrip> scrips = Collections.emptyList();

	@Override
	public List<Scrip> findAll() {
		return scrips;
	}

	@Override
	public Page<Scrip> findByIndices(Index index, Pageable pageable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long countByIndices(Index index) {
		return scrips.stream()
				.filter(scrip -> scrip.getIndices().contains(index))
				.count();
	}

	@Override
	public List<Scrip> findByIndices(Index index) {
		return scrips.stream()
				.filter(scrip -> scrip.getIndices().contains(index))
				.collect(Collectors.toList());
	}

	@Override
	public List<Scrip> saveAll(List<Scrip> scrips) {
		this.scrips = scrips;
		return scrips;
	}

	@Override
	public Scrip findByCode(String code) {
		return scrips.stream()
				.filter(scrip -> Objects.equals(code, scrip.getCode()))
				.findFirst().orElse(null);
	}

}

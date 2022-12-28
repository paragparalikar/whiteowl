package com.whiteowl.strategy.test.mock;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;

public class MockScripService implements ScripService {

	@Override
	public List<Scrip> findAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Page<Scrip> findByIndices(Index index, Pageable pageable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long countByIndices(Index index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Scrip> findByIndices(Index index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Scrip> saveAll(List<Scrip> scrips) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Scrip findByCode(String code) {
		throw new UnsupportedOperationException();
	}

}

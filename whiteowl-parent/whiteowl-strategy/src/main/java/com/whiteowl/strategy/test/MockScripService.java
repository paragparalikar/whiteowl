package com.whiteowl.strategy.test;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;

public class MockScripService implements ScripService {

	public MockScripService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<Scrip> findAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<Scrip> findByIndices(Index index, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long countByIndices(Index index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Scrip> findByIndices(Index index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Scrip> saveAll(List<Scrip> scrips) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Scrip findByCode(String code) {
		// TODO Auto-generated method stub
		return null;
	}

}

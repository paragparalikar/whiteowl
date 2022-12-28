package com.whiteowl.core.scrip;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScripService {

	List<Scrip> findAll();

	Page<Scrip> findByIndices(Index index, Pageable pageable);

	long countByIndices(Index index);

	List<Scrip> findByIndices(Index index);

	List<Scrip> saveAll(List<Scrip> scrips);

	Scrip findByCode(String code);

}
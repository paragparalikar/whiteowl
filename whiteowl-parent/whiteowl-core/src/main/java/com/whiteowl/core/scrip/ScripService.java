package com.whiteowl.core.scrip;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScripService {

	@NonNull private final ScripRepository scripRepository;
	
	public Page<Scrip> findByIndices(Index index, Pageable pageable) {
		return scripRepository.findByIndices(index, pageable);
	}
	
	public long countByIndices(Index index) {
		return scripRepository.countByIndices(index);
	}
	
	public List<Scrip> findByIndices(@NonNull Index index) {
		return scripRepository.findByIndices(index);
	}
	
	@CacheEvict(cacheNames = "scrips-by-code", allEntries =  true, beforeInvocation = true)
	public void saveAll(List<Scrip> scrips) {
		scripRepository.saveAll(scrips);
	}

	@Cacheable(cacheNames = "scrips-by-code")
	public Scrip findByCode(String code) {
		return scripRepository.findByCode(code).orElse(null);
	}
}

package com.whiteowl.core.scrip;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScripService {

	@NonNull private final ScripRepository scripRepository;
	
	@Cacheable(cacheNames = "scrips-all", key = "'all'")
	public List<Scrip> findAll(){
		return scripRepository.findAll();
	}
	
	public Page<Scrip> findByIndices(Index index, Pageable pageable) {
		return scripRepository.findByIndices(index, pageable);
	}
	
	public long countByIndices(Index index) {
		return scripRepository.countByIndices(index);
	}
	
	@Cacheable(cacheNames = "scrips-all")
	public List<Scrip> findByIndices(@NonNull Index index) {
		return scripRepository.findByIndices(index);
	}
	
	@Caching(
			put = @CachePut(key = "'all'", cacheNames = "scrips-all"),
			evict = @CacheEvict(cacheNames = "scrips-by-code", allEntries =  true))
	public List<Scrip> saveAll(List<Scrip> scrips) {
		return scripRepository.saveAllAndFlush(scrips);
	}

	//@Cacheable(cacheNames = "scrips-by-code")
	public Scrip findByCode(String code) {
		return scripRepository.findByCode(code).orElse(null);
	}
}

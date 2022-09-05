package com.whiteowl.core.scrip;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScripRepository extends JpaRepository<Scrip, String> {

	Optional<Scrip> findByCode(String code);
	
	List<Scrip> findByIndices(Index index);
	
	Page<Scrip> findByIndices(Index index, Pageable pageable);
	
	long countByIndices(Index index);
}

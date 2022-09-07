package com.whiteowl.core.scrip;

import java.util.List;
import java.util.Optional;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
@EnableScan
public interface ScripRepository extends CrudRepository<Scrip, String> {

	Optional<Scrip> findByCode(String code);
	
	List<Scrip> findByIndices(Index index);
	
	Page<Scrip> findByIndices(Index index, Pageable pageable);
	
	long countByIndices(Index index);
}

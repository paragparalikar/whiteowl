package com.whiteowl.core.bar;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BarRepository extends JpaRepository<PersistentBar, PersistentBarKey> {
	
	Optional<PersistentBar> findTopByCodeAndTimeframeOrderByTimeframeDesc(String code, Timeframe timeframe);
	
	List<PersistentBar> findByCodeAndTimeframe(String code, Timeframe timeframe);
	
	List<PersistentBar> findByCodeAndTimeframe(String code, Timeframe timeframe, Pageable pageable);
	
	List<PersistentBar> findByCodeAndTimeframeAndBeginTimeBefore(String code, Timeframe timeframe, 
			ZonedDateTime from, Pageable pageable);
	
	List<PersistentBar> findByCodeAndTimeframeAndBeginTimeBetween(String code, Timeframe timeframe, 
			ZonedDateTime from, ZonedDateTime to);

	@Query(value = "select max(b.beginTime) from PersistentBar b where b.code = ?1 and b.timeframe = ?2")
	Optional<ZonedDateTime> findMaxBeginTimeByCodeAndTimeframe(String code, Timeframe timeframe);
}


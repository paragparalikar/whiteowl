package com.whiteowl.core.bar;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultBarService implements BarService {

	private final BarRepository barRepository;
	
	@Override
	public List<PersistentBar> findByCodeAndTimeframe(String code, Timeframe timeframe) {
		return barRepository.findByCodeAndTimeframe(code, timeframe);
	}
	
	@Override
	public Optional<ZonedDateTime> findMaxBeginTimeByCodeAndTimeframe(String code, Timeframe timeframe){
		return barRepository.findMaxBeginTimeByCodeAndTimeframe(code, timeframe);
	}
	
	@Override
	public Optional<PersistentBar> findTopByCodeAndTimeframeOrderByTimeframeDesc(String code, Timeframe timeframe) {
		return barRepository.findTopByCodeAndTimeframeOrderByTimeframeDesc(code, timeframe);
	}
	
	@Override
	public List<PersistentBar> findLatestByCodeAndTimeframe(String code, Timeframe timeframe, long count){
		final Sort sort = Sort.by(Direction.DESC, "beginTime");
		final Pageable pageable = PageRequest.of(0, (int) count, sort);
		return barRepository.findByCodeAndTimeframe(code, timeframe, pageable);
	}
	
	@Override
	public PersistentBar saveSafe(PersistentBar bar) {
		try {
			return barRepository.saveAndFlush(bar);
		}catch(DataIntegrityViolationException dive) {
			log.warn("Bar already exists : code - {}, duration - {} minutes, endTime - {}",
					bar.getCode(), bar.getTimeframe(), bar.getBeginTime());
		}
		return bar;
	}
	
	@Override
	public Optional<PersistentBar> findLatestBar(String code, Timeframe timeframe){
		return findTopByCodeAndTimeframeOrderByTimeframeDesc(code, timeframe);
	}

}

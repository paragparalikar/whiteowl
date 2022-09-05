package com.whiteowl.core.bar;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface BarService {

	List<PersistentBar> findByCodeAndTimeframe(String code, Timeframe timeframe);

	Optional<ZonedDateTime> findMaxBeginTimeByCodeAndTimeframe(String code, Timeframe timeframe);

	Optional<PersistentBar> findTopByCodeAndTimeframeOrderByTimeframeDesc(String code, Timeframe timeframe);

	List<PersistentBar> findLatestByCodeAndTimeframe(String code, Timeframe timeframe, long count);

	PersistentBar saveSafe(PersistentBar bar);

	Optional<PersistentBar> findLatestBar(String code, Timeframe timeframe);

}
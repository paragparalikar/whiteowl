package com.whiteowl.core.strategy.backtester;

import java.util.List;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class BackTestReport {

	private final List<Position> positions;
	private final TradingStrategyConfig config;
	

}

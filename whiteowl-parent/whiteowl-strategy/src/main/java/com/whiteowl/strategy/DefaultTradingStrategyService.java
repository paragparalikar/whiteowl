package com.whiteowl.strategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.strategy.shortstrangle.ShortStraddleConfig;
import com.whiteowl.strategy.shortstrangle.ShortStraddleTradingStrategy;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultTradingStrategyService implements TradingStrategyService {
	
	private final ScripService scripService;
	private final PositionService positionService;
	private final OptionChainService optionChainService;
	private final Map<TradingStrategyTemplate, BiFunction<Scrip, TradingStrategyConfig, 
		Optional<TradingStrategy>>> builders = new HashMap<>();
	
	@PostConstruct
	public void postConstruct() {
		builders.put(TradingStrategyTemplate.SHORT_STRADDLE, this::buildShortStraddle);
	}
	
	@Override
	public void execute(@NonNull TradingStrategyConfig config) {
		final BiFunction<Scrip, TradingStrategyConfig, Optional<TradingStrategy>>
			builder = builders.get(config.getTemplate());
		if(null != builder) {
			for(Scrip scrip : scripService.findAll().stream()
					.filter(config.getScripCriteria()).toList()) {
				final TradingStrategy strategy = builder.apply(scrip, config).orElse(null);
				if(null != strategy) execute(scrip, config, strategy);
			}
		}
	}
	
	private void execute(Scrip scrip, TradingStrategyConfig config, TradingStrategy tradingStrategy) {
		resolvePositions(scrip, config).stream()
			.filter(tradingStrategy::handle)
			.map(positionService::save);
	}
	
	private List<Position> resolvePositions(Scrip scrip, TradingStrategyConfig config){
		final List<Position> positions = positionService.findByScripAndTradingStrategyConfigIdAndStatus(
				scrip, config.getId(), PositionStatus.OPEN);
		if(positions.isEmpty()) positions.add(createNewPosition(scrip, config));
		return positions;
	}
	
	private Position createNewPosition(Scrip scrip, TradingStrategyConfig config) {
		final Position position = new Position();
		position.setScrip(scrip);
		position.setStatus(PositionStatus.NEW);
		position.setTradingStrategyConfigId(config.getId());
		return position;
	}
	
	private Optional<TradingStrategy> buildShortStraddle(
			@NonNull Scrip scrip, 
			@NonNull TradingStrategyConfig config) {
		return optionChainService.findByScrip(scrip)
			.map(optionChain -> new ShortStraddleTradingStrategy(
					optionChain, (ShortStraddleConfig) config));
	}
	
}

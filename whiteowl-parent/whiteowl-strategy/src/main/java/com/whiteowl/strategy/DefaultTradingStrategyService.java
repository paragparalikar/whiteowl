package com.whiteowl.strategy;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultTradingStrategyService implements TradingStrategyService {
	
	private final ScripService scripService;
	private final OptionChainService optionChainService;
	
	@Override
	public void execute(@NonNull TradingStrategyConfig config) {
		
	}
	
	private Optional<TradingStrategy> buildShortStraddle(Scrip scrip) {
		return null;
	}
	
}

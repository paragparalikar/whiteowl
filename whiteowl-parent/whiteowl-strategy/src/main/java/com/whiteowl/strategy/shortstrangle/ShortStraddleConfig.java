package com.whiteowl.strategy.shortstrangle;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.strategy.TradingStrategyTemplate;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortStraddleConfig implements TradingStrategyConfig {
	
	@PositiveOrZero private Integer quantity = 1;
	@PositiveOrZero private Double percentageStopLoss = 25D;
	@NotBlank private String positionOpenCron = "0 16 9 * * MON-FRI";
	@NotBlank private String positionCloseCron = "0 15 15 * * MON-FRI";
	
	private final int minBarCount = 0;
	private final Timeframe timeframe = Timeframe.D;
	private final String scripCode = Index.NIFTY50.getCode();
	private final TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.SHORT_STRADDLE;
	
	@Override
	public Set<String> getCronExpressions() {
		return Stream.of(positionOpenCron, positionCloseCron).collect(Collectors.toSet());
	}
}

package com.whiteowl.strategy.shortstrangle;

import java.time.LocalTime;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import com.whiteowl.core.scrip.Index;
import com.whiteowl.strategy.TradingStrategyTemplate;
import com.whiteowl.strategy.config.TradingStrategyConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortStraddleConfig implements TradingStrategyConfig {
	
	public String getId() {
		return Index.NIFTY50.getCode() + TradingStrategyTemplate.SHORT_STRADDLE.getId();
	}
	
	private boolean enabled = Boolean.TRUE;
	@PositiveOrZero private Integer quantity = 1;
	@PositiveOrZero private Integer minBarCount = 0;
	@PositiveOrZero private Double percentageTarget = 999D;
	@PositiveOrZero private Double percentageStopLoss = 25D;
	@NotNull @NonNull private LocalTime minPositionOpenTime = LocalTime.of(9, 15);
	@NotNull @NonNull private LocalTime maxPositionOpenTime = LocalTime.of(9, 16);
	@NotNull @NonNull private LocalTime minPositionCloseTime = LocalTime.of(15, 29);
	@NotNull @NonNull private LocalTime maxPositionCloseTime = LocalTime.of(15, 30);
	private final TradingStrategyTemplate template = TradingStrategyTemplate.SHORT_STRADDLE;
	
}

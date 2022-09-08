package com.whiteowl.strategy.shortstrangle;

import java.time.LocalTime;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.ScripCriteria;
import com.whiteowl.strategy.TradingStrategyConfig;
import com.whiteowl.strategy.TradingStrategyTemplate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortStraddleConfig implements TradingStrategyConfig {

	private Long id;
	private final TradingStrategyTemplate template = TradingStrategyTemplate.SHORT_STRADDLE;
	private final ScripCriteria scripCriteria = new ScripCriteria().withCode(Index.NIFTY50.getCode());
	
	private boolean enabled = Boolean.TRUE;
	@PositiveOrZero private Integer quantity = 1;
	@PositiveOrZero private Integer minBarCount = 0;
	@PositiveOrZero private Double percentageTarget = 40D;
	@PositiveOrZero private Double percentageStopLoss = 20D;
	@NotNull @NonNull private LocalTime minPositionOpenTime = LocalTime.of(9, 20);
	@NotNull @NonNull private LocalTime maxPositionOpenTime = LocalTime.of(14, 0);
	@NotNull @NonNull private LocalTime maxPositionCloseTime = LocalTime.of(15, 25);
	
}

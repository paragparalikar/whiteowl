package com.whiteowl.strategy.test;

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.strategy.TradingStrategyExecutor;
import com.whiteowl.strategy.test.mock.MockBarService;
import com.whiteowl.strategy.test.mock.MockBrokerServiceProvider;
import com.whiteowl.strategy.test.mock.MockQuoteService;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class BackTestExecutor {

	private final Scrip scrip;
	private final Portfolio portfolio;
	private final Timeframe timeframe;
	private final MockBarService barService;
	private final MockQuoteService quoteService;
	private final PositionService positionService;
	private final EquityCurveObserver equityCurveObserver;
	private final MockBrokerServiceProvider brokerServiceProvider;
	private final TradingStrategyExecutor tradingStrategyExecutor;
	
	public void execute() {
		while(barService.next()) {
			final Bar bar = barService.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
					scrip.getCode(), timeframe, 1).getLastBar();
			quoteService.publish(bar, scrip, TradeType.BUY);
			tradingStrategyExecutor.onScripBarDownloaded(scrip, timeframe);
			brokerServiceProvider.execute();
			positionService.findByPortfolioAndStatusNot(portfolio, PositionStatus.CLOSED).stream()
				.map(positionService::save) // The position might have been modified by broker outside position service
				.forEach(tradingStrategyExecutor::onPositionSynchronized);
			equityCurveObserver.next(bar.getClosePrice().doubleValue());
		}
	}

}

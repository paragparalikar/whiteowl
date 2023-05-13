package com.whiteowl.core.analysis.backtester;

import java.util.Arrays;
import java.util.List;

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.bar.event.BarCreatedEvent;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.context.BarSeriesCacheManager;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;

@Value
@Builder
public class Backtest {
	
	private final Scrip scrip;
	private final List<Bar> bars;
	private final Timeframe timeframe;
	private final double initialMargin, slippagePercentage;
	
	@SneakyThrows
	public List<Position> execute(TradingStrategyConfig config){
		final MockTradingStrategyContext context = new MockTradingStrategyContext(scrip, config, initialMargin, slippagePercentage);
		final MockPositionService positionService = context.getMockPositionService();
		final BarSeriesCacheManager barSeriesCacheManager = context.getBarSeriesCacheManager();
		final MockBrokerServiceProvider brokerServiceProvider = context.getMockBrokerServiceProvider();
		final TradingStrategy tradingStrategy = config.createTradingStrategy(scrip, timeframe, context);
		for(int barIndex = 0; barIndex < bars.size(); barIndex++) {
			final Bar bar = bars.get(barIndex);
			final Bar tradeExecutionBar = barIndex < bars.size() - 1 ? bars.get(barIndex + 1) : bar;
			final List<Quote> quotes = createQuotes(scrip.getCode(), bar, config.getTradeType());
			for(int quoteIndex = 0; quoteIndex < quotes.size(); quoteIndex++) {
				final Quote quote = quotes.get(quoteIndex);
				context.getMockQuoteService().push(scrip.getCode(), quote);
				brokerServiceProvider.execute(tradeExecutionBar, scrip);
				positionService.updateAll();
			}
			final BarCreatedEvent barCreatedEvent = new BarCreatedEvent(bar, scrip, timeframe);
			barSeriesCacheManager.onBarCreated(barCreatedEvent);
			brokerServiceProvider.execute(tradeExecutionBar, scrip);
			positionService.updateAll();
		}
		tradingStrategy.close();
		final Bar lastBar = bars.get(bars.size() - 1);
		positionService.closeAll(lastBar.getClosePrice().doubleValue(), 
				lastBar.getEndTime().toLocalDateTime());
		return positionService.findAll();
	}
	
	private List<Quote> createQuotes(String code, Bar bar, TradeType tradeType){
		final Quote openQuote = Quote.builder().code(code).lastPrice(bar.getOpenPrice().doubleValue()).build();
		final Quote highQuote = Quote.builder().code(code).lastPrice(bar.getHighPrice().doubleValue()).build();
		final Quote lowQuote = Quote.builder().code(code).lastPrice(bar.getLowPrice().doubleValue()).build();
		final Quote closeQuote = Quote.builder().code(code).lastPrice(bar.getClosePrice().doubleValue()).build();
		return TradeType.BUY.equals(tradeType) ? 
				Arrays.asList(openQuote, lowQuote, highQuote, closeQuote) :
				Arrays.asList(openQuote, highQuote, lowQuote, closeQuote);
	}

}

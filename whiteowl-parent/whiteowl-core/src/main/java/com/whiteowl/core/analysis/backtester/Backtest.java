package com.whiteowl.core.analysis.backtester;

import java.util.Arrays;
import java.util.List;

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.bar.event.BarCreatedEvent;
import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.context.BarSeriesCacheManager;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Value
@Builder
public class Backtest {
	
	private final Scrip scrip;
	private final List<Bar> bars;
	private final Timeframe timeframe;
	private final double initialMargin, slippagePercentage;
	
	private Portfolio createPortfolio() {
		final Portfolio portfolio = new Portfolio();
		portfolio.setBroker(Broker.TEST);
		portfolio.setAvailableMargin(initialMargin);
		portfolio.setMaxTradableAmount(initialMargin);
		return portfolio;
	}
	
	@SneakyThrows
	public List<Position> execute(TradingStrategyConfig config){
		final MockTradingStrategyContext tradingStrategyContext = new MockTradingStrategyContext(initialMargin, slippagePercentage);
		final MockQuoteService mockQuoteService = tradingStrategyContext.getMockQuoteService();
		final MockScripService mockScripService = tradingStrategyContext.getMockScripService();
		final MockPositionService mockPositionService = tradingStrategyContext.getMockPositionService();
		final MockPortfolioService mockPortfolioService = tradingStrategyContext.getMockPortfolioService();
		final MockTradingStrategyConfigService mockTradingStrategyConfigService = tradingStrategyContext.getMockTradingStrategyConfigService();
		final BarSeriesCacheManager barSeriesCacheManager = tradingStrategyContext.getBarSeriesCacheManager();
		final MockBrokerServiceProvider mockBrokerServiceProvider = tradingStrategyContext.getMockBrokerServiceProvider();
		mockPortfolioService.save(createPortfolio());
		mockScripService.saveAll(Arrays.asList(scrip));
		mockTradingStrategyConfigService.save(config);
		barSeriesCacheManager.init();
		final TradingStrategy tradingStrategy = config.createTradingStrategy(scrip, timeframe, tradingStrategyContext);
		for(Bar bar : bars) {
			mockBrokerServiceProvider.execute(bar, scrip);
			final List<Quote> quotes = createQuotes(scrip.getCode(), bar, config.getTradeType());
			quotes.forEach(quote -> mockQuoteService.push(scrip.getCode(), quote));
			final BarCreatedEvent barCreatedEvent = new BarCreatedEvent(bar, scrip, timeframe);
			barSeriesCacheManager.onBarCreated(barCreatedEvent);
		}
		tradingStrategy.close();
		final Bar lastBar = bars.get(bars.size() - 1);
		mockPositionService.closeAll(lastBar.getClosePrice().doubleValue(), 
				lastBar.getEndTime().toLocalDateTime());
		final List<Position> positions = mockPositionService.findAll();
		log.info("Backtest found {} trades for scrip {}", positions.size(), scrip.getCode());
		return positions;
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

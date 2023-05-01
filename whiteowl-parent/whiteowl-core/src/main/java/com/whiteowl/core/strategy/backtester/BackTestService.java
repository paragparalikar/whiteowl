package com.whiteowl.core.strategy.backtester;

import java.util.Arrays;
import java.util.List;

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.event.BarCreatedEvent;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Builder
@RequiredArgsConstructor
public class BackTestService {

	@SneakyThrows
	public BackTestReport backtest(TradingStrategyConfig config, List<Bar> bars, double initialMargine, double slippagePercentage) {
		final Scrip scrip = Scrip.builder().code(config.getScripCode()).build();
		final MockTradingStrategyContext tradingStrategyContext = new MockTradingStrategyContext(initialMargine, slippagePercentage);
		tradingStrategyContext.getMockTradingStrategyConfigService().save(config);
		tradingStrategyContext.getBarSeriesCacheManager().init();
		final TradingStrategy tradingStrategy = config.createTradingStrategy(tradingStrategyContext);
		for(Bar bar : bars) {
			final List<Quote> quotes = createQuotes(config.getScripCode(), bar, config.getTradeType());
			quotes.forEach(quote -> tradingStrategyContext.getMockQuoteService().push(config.getScripCode(), quote));
			final BarCreatedEvent barCreatedEvent = new BarCreatedEvent(bar, scrip, config.getTimeframe());
			tradingStrategyContext.getBarSeriesCacheManager().onBarCreated(barCreatedEvent);
		}
		tradingStrategy.close();
		final List<Position> positions = tradingStrategyContext.getMockPositionService().findAll();
		return BackTestReport.builder().config(config).positions(positions).build();
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

package com.whiteowl.core.analysis.backtester;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;
import org.ta4j.core.num.Num;

import com.whiteowl.core.analysis.backtester.listener.BacktestListener;
import com.whiteowl.core.analysis.backtester.mock.MockBrokerServiceProvider;
import com.whiteowl.core.analysis.backtester.mock.MockPositionService;
import com.whiteowl.core.analysis.backtester.mock.MockTradingStrategyContext;
import com.whiteowl.core.analysis.performance.TradingStrategyConfigPerformance;
import com.whiteowl.core.bar.JdbcBarRepository;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.bar.event.BarCreatedEvent;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripType;
import com.whiteowl.core.strategy.TradingStrategy;
import com.whiteowl.core.strategy.config.TradingStrategyConfig;
import com.whiteowl.core.strategy.context.BarSeriesCacheManager;
import com.whiteowl.core.strategy.impl.MACDLongTradingStrategyConfig;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;

@Value
@Builder
public class Backtest {
	
	private final Scrip scrip;
	private final List<Bar> bars;
	private final Timeframe timeframe;
	private final TradingStrategyConfig config;
	private final double initialMargin, slippagePercentage;
	
	@SneakyThrows
	public void execute(BacktestListener backtestListener){
		final MockTradingStrategyContext context = new MockTradingStrategyContext(scrip, config, backtestListener, initialMargin, slippagePercentage);
		final MockPositionService positionService = context.getMockPositionService();
		final BarSeriesCacheManager barSeriesCacheManager = context.getBarSeriesCacheManager();
		final MockBrokerServiceProvider brokerServiceProvider = context.getMockBrokerServiceProvider();
		final TradingStrategy tradingStrategy = config.createTradingStrategy(scrip, timeframe, context);
		backtestListener.onStart(this);
		for(int barIndex = 0; barIndex < bars.size(); barIndex++) {
			final Bar bar = bars.get(barIndex);
			final List<Quote> quotes = createQuotes(scrip.getCode(), bar, config.getTradeType());
			for(int quoteIndex = 0; quoteIndex < quotes.size(); quoteIndex++) {
				final Quote quote = quotes.get(quoteIndex);
				context.getMockQuoteService().push(scrip.getCode(), quote);
				brokerServiceProvider.execute(quote);
				positionService.updateAll();
				backtestListener.onQuote(quote);
			}
			final BarCreatedEvent barCreatedEvent = new BarCreatedEvent(bar, scrip, timeframe);
			barSeriesCacheManager.onBarCreated(barCreatedEvent);
			backtestListener.onBar(bar);
		}
		tradingStrategy.close();
		final Bar lastBar = bars.get(bars.size() - 1);
		positionService.closeAll(lastBar.getClosePrice().doubleValue(), 
				lastBar.getEndTime().toLocalDateTime());
		backtestListener.onEnd(this);
	}
	
	private List<Quote> createQuotes(String code, Bar bar, TradeType tradeType){
		final Quote openQuote = createQuote(code, bar.getOpenPrice(), bar.getBeginTime());
		final Quote highQuote = createQuote(code, bar.getHighPrice(), bar.getEndTime());
		final Quote lowQuote = createQuote(code, bar.getLowPrice(), bar.getEndTime());
		final Quote closeQuote = createQuote(code, bar.getClosePrice(), bar.getEndTime());
		return TradeType.BUY.equals(tradeType) ? 
				Arrays.asList(openQuote, lowQuote, highQuote, closeQuote) :
				Arrays.asList(openQuote, highQuote, lowQuote, closeQuote);
	}
	
	private Quote createQuote(String code, Num lastPrice, ZonedDateTime lastTradeTime) {
		return Quote.builder()
				.code(code)
				.lastPrice(lastPrice.doubleValue())
				.lastTradeTime(lastTradeTime.toLocalDateTime())
				.build();
	}
	
	public static void main(String[] args) {
		final String code = "RELIANCE";
		final Timeframe timeframe = Timeframe.M15;
		final double initialMargin = 1_00;
		final double slippagePercentage = 0;
		final Scrip scrip = Scrip.builder().type(ScripType.EQ).exchange(Exchange.NSE).code(code).build();
		final TradingStrategyConfig config = MACDLongTradingStrategyConfig.builder()
				.longBarCount(26)
				.shortBarCount(12)
				.signalBarCount(9)
				.build();
		final List<Bar> bars = JdbcBarRepository.instance().findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
				code, timeframe, 300 * timeframe.getDayMultiple(), 0);
		final ZonedDateTime startTimestamp = ZonedDateTime.of(2022, 7, 6, 13, 30, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime endTimestamp = ZonedDateTime.of(2023, 5, 15, 9, 15, 0, 0, ZoneId.systemDefault());
		bars.removeIf(bar -> bar.getBeginTime().isBefore(startTimestamp) || bar.getEndTime().isAfter(endTimestamp));
		final TradingStrategyConfigPerformance performance = new TradingStrategyConfigPerformance(initialMargin);
		Backtest.builder()
			.bars(bars)
			.scrip(scrip)
			.config(config)
			.timeframe(timeframe)
			.initialMargin(initialMargin)
			.slippagePercentage(slippagePercentage)
			.build()
			.execute(performance);
		System.out.println(performance);
	}

}

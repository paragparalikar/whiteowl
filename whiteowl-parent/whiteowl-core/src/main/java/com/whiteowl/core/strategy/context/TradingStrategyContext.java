package com.whiteowl.core.strategy.context;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import org.ta4j.core.BarSeries;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.scrip.Scrip;

public interface TradingStrategyContext {
	
	ScheduledFuture<?> schedule(Runnable runnable, String cronExpression);
	
	void unsubscribe(Runnable barListener);
	
	BarSeries getBarSeries(String scripCode, Timeframe timeframe);

	void subscribe(Scrip scrip, Timeframe timeframe, Runnable barListener);
	
	void unsubscribe(Consumer<Quote> quoteListener);
	
	void subscribe(Collection<Scrip> scrips, QuoteMode mode, Consumer<Quote> quoteListener);
	
	Scrip getScrip(String scripCode);
	
	Position save(Position position);
	
	List<Position> getOpenPositions(String tradingStrategyConfigId);
	
}

package com.whiteowl.strategy.shortstrangle;

import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.whiteowl.core.derivative.option.OptionChain;
import com.whiteowl.core.derivative.option.OptionChainItem;
import com.whiteowl.core.derivative.option.OptionChainService;
import com.whiteowl.core.derivative.option.OptionInfo;
import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.quote.Quote;
import com.whiteowl.core.quote.QuoteMode;
import com.whiteowl.core.quote.QuoteService;
import com.whiteowl.core.quote.QuoteSubscription;
import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;
import com.whiteowl.core.scrip.ScripType;
import com.whiteowl.strategy.TradingStrategyExecutor;
import com.whiteowl.strategy.TradingStrategyTemplate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShortStraddleTradingStrategyExecutor implements TradingStrategyExecutor<ShortStraddleConfig> {

	private final ScripService scripService;
	private final QuoteService quoteService;
	private final PositionService positionService;
	private final OptionChainService optionChainService;
	@Getter private final TradingStrategyTemplate tradingStrategyTemplate = TradingStrategyTemplate.SHORT_STRADDLE;

	private QuoteSubscription callQuoteSubscription, putQuoteSubscription;
	
	@Override
	public void schedule(ShortStraddleConfig config, TaskScheduler taskScheduler) {
		taskScheduler.schedule(() -> openPosition(config), new CronTrigger(config.getPositionOpenCron()));
		taskScheduler.schedule(() -> closePosition(config), new CronTrigger(config.getPositionCloseCron()));
	}
	
	private void openPosition(ShortStraddleConfig config) {
		Optional.ofNullable(putQuoteSubscription).ifPresent(QuoteSubscription::unsubscribe);
		Optional.ofNullable(callQuoteSubscription).ifPresent(QuoteSubscription::unsubscribe);
		final Scrip scrip = scripService.findByCode(Index.NIFTY50.getCode());
		final OptionChain optionChain = optionChainService.findByScrip(scrip).orElseThrow();
		final OptionInfo optionInfo = optionChain.findByDeltaAndScripType(0.5, ScripType.CE).orElseThrow();
		final OptionChainItem item = optionChain.findByStrikePrice(optionInfo.getStrikePrice()).orElseThrow();
		final OptionInfo callInfo = item.getCallOptionInfo();
		final OptionInfo putInfo = item.getPutOptionInfo();
		putQuoteSubscription = quoteService.subscribe(putInfo.getScrip(), QuoteMode.LTP);
		callQuoteSubscription = quoteService.subscribe(callInfo.getScrip(), QuoteMode.LTP);
	}
	
	private void onQuote(ShortStraddleConfig config, Quote quote) {
		final List<Position> positions = positionService.findByTradingStrategyConfigId(config.getId());
		
	}
	
	
	private void closePosition(ShortStraddleConfig config) {
		
	}
	
	@Override
	public void close() throws Exception {
		
	}
}

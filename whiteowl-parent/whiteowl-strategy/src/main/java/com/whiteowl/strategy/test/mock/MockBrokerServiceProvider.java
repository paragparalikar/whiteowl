package com.whiteowl.strategy.test.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.bar.BarService;
import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.broker.BrokerServiceProvider;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Exchange;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeLimitType;
import com.whiteowl.core.trade.TradeProduct;
import com.whiteowl.core.trade.TradeStatus;
import com.whiteowl.core.trade.TradeValidity;
import com.whiteowl.core.util.Constant;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MockBrokerServiceProvider implements BrokerServiceProvider {

	private final Timeframe timeframe;
	private final BarService barService;
	private final AtomicLong idGenerator = new AtomicLong();
	private final Map<Long, Trade> trades = new ConcurrentHashMap<>();
	
	@Override
	public Broker getBrokerType() {
		return Broker.TEST;
	}
	
	@Override
	public double getAvailableMargin(Portfolio portfolio) {
		return portfolio.getAvailableMargin();
	}

	@Override
	public List<Trade> findAllTrades(Portfolio portfolio) {
		return new ArrayList<>(trades.values());
	}

	@Override
	public void create(Trade trade, Portfolio portfolio) {
		if(!TradeStatus.PENDING.equals(trade.getStatus())) throw new IllegalStateException();
		if(null == trade.getId()) trade.setId(idGenerator.incrementAndGet());
		final Bar bar = barService.findLatestBar(trade.getScrip().getCode(), timeframe).orElseThrow();
		trade.setTimestamp(bar.getEndTime().toLocalDateTime());
		trade.setStatus(TradeStatus.OPEN);
		trades.put(trade.getId(), trade);
		final double executionPrice = resolveExecutionPrice(trade, bar);
		portfolio.setAvailableMargin(portfolio.getAvailableMargin() + 
				executionPrice * (TradeType.BUY.equals(trade.getType()) ? - 1 : 1));
	}
	
	@Override
	public void update(Trade trade, Portfolio portfolio) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void cancel(Trade trade, Portfolio portfolio) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getAvailableQuantity(Scrip scrip, Exchange exchange, TradeProduct product, Portfolio portfolio) {
		throw new UnsupportedOperationException();
	}
	
	public void execute(@NonNull final Timeframe timeframe) {
		for(Trade trade : trades.values()) {
			if(TradeStatus.OPEN.equals(trade.getStatus())) {
				barService.findLatestBar(trade.getScrip().getCode(), timeframe)
					.ifPresent(bar -> execute(trade, bar));
			}
		}
	}
	
	private void execute(Trade trade, Bar bar) {
		if(TradeProduct.MIS.equals(trade.getProduct())) {
			if(!bar.getEndTime().toLocalTime().isBefore(Constant.ZERODHA_SQUARE_OFF_TIME)) {
				trade.setStatus(TradeStatus.CANCELLED);
				return;
			}
		}
		if(TradeValidity.IOC.equals(trade.getValidity())) {
			throw new UnsupportedOperationException();
		} else if(TradeValidity.DAY.equals(trade.getValidity())) {
			if(bar.getBeginTime().toLocalDate().isAfter(trade.getTimestamp().toLocalDate())) {
				trade.setStatus(TradeStatus.CANCELLED);
				return;
			}
		}
		if(TradeLimitType.LIMIT.equals(trade.getLimitType())) {
			if(TradeType.BUY.equals(trade.getType())) {
				if(bar.getLowPrice().doubleValue() < trade.getPrice()) {
					complete(trade, bar);
					return;
				}
			} else if(TradeType.SELL.equals(trade.getType())) {
				if(bar.getHighPrice().doubleValue() > trade.getPrice()) {
					complete(trade, bar);
					return;
				}
			}
		} else if(TradeLimitType.MARKET.equals(trade.getLimitType())) {
			complete(trade, bar);
			return;
		} else if(TradeLimitType.SL.equals(trade.getLimitType())) {
			if(TradeType.BUY.equals(trade.getType())) {
				if(bar.getHighPrice().doubleValue() > trade.getTriggerPrice()) {
					if(bar.getLowPrice().doubleValue() < trade.getPrice()) {
						complete(trade, bar);
						return;
					}
				}
			} else if(TradeType.SELL.equals(trade.getType())) {
				if(bar.getLowPrice().doubleValue() < trade.getTriggerPrice()) {
					if(bar.getHighPrice().doubleValue() > trade.getPrice()) {
						complete(trade, bar);
						return;
					}
				}
			}
		} else if(TradeLimitType.SLM.equals(trade.getLimitType())) {
			if(TradeType.BUY.equals(trade.getType())) {
				if(bar.getHighPrice().doubleValue() > trade.getTriggerPrice()) {
					complete(trade, bar);
					return;
				}
			} else if(TradeType.SELL.equals(trade.getType())) {
				if(bar.getLowPrice().doubleValue() < trade.getTriggerPrice()) {
					complete(trade, bar);
					return;
				}
			}
		}
	}
	
	private double resolveExecutionPrice(Trade trade, Bar bar) {
		final TradeLimitType limitType = trade.getLimitType();
		if(TradeLimitType.LIMIT.equals(limitType) || TradeLimitType.SL.equals(limitType)) {
			return trade.getPrice();
		} else if(TradeLimitType.MARKET.equals(limitType) || TradeLimitType.SLM.equals(limitType)) {
			return TradeType.BUY.equals(trade.getType()) ?
					bar.getHighPrice().doubleValue() :
					bar.getLowPrice().doubleValue();
		}
		throw new IllegalArgumentException("Could not resolve execution price");
	}

	private void complete(Trade trade, Bar bar) {
		final double price = resolveExecutionPrice(trade, bar);
		trade.setAveragePrice(price);
		trade.setStatus(TradeStatus.COMPLETE);
		trade.setFilledQuantity(trade.getQuantity());
	}
	
}

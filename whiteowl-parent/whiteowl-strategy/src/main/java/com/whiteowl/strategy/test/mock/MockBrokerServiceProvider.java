package com.whiteowl.strategy.test.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	private final Portfolio portfolio;
	private final BarService barService;
	private final Set<Trade> trades = new HashSet<>();
	private final AtomicLong idGenerator = new AtomicLong();
	private final Map<Scrip, Integer> onBalanceQuantities = new HashMap<>();
	
	@Override
	public Broker getBrokerType() {
		return Broker.TEST;
	}
	
	@Override
	public double getAvailableMargin(@NonNull final Portfolio portfolio) {
		return 0 == portfolio.getAvailableMargin() ? 
				portfolio.getMaxTradableAmount() : portfolio.getAvailableMargin();
	}

	@Override
	public List<Trade> findAllTrades(@NonNull final Portfolio portfolio) {
		return new ArrayList<>(trades);
	}

	@Override
	public void create(@NonNull final Trade trade, @NonNull final Portfolio portfolio) {
		if(!TradeStatus.PENDING.equals(trade.getStatus())) throw new IllegalStateException();
		if(null == trade.getId()) trade.setId(idGenerator.incrementAndGet());
		final Bar bar = barService.findLatestBar(trade.getScrip().getCode(), timeframe).orElseThrow();
		trade.setTimestamp(bar.getEndTime().toLocalDateTime());
		trade.setStatus(TradeStatus.OPEN);
		trades.add(trade);
		updateAvailableMargin(trade, bar);
	}
	
	@Override
	public void update(@NonNull final Trade trade, @NonNull final Portfolio portfolio) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void cancel(@NonNull final Trade trade, @NonNull final Portfolio portfolio) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getAvailableQuantity(
			@NonNull final Scrip scrip, @NonNull final Exchange exchange, 
			@NonNull final TradeProduct product, @NonNull final Portfolio portfolio) {
		throw new UnsupportedOperationException();
	}
	
	public void execute() {
		trades.stream()
			.filter(trade -> TradeStatus.OPEN.equals(trade.getStatus()))
			.forEach(this::execute);
	}
	
	private void execute(Trade trade) {
		final Bar bar = barService.findLatestBar(trade.getScrip().getCode(), timeframe).orElseThrow();
		if(TradeLimitType.MARKET.equals(trade.getLimitType())) {
			trigger(trade, bar);
			return;
		}
		if(TradeProduct.MIS.equals(trade.getProduct())) {
			if(!bar.getEndTime().toLocalTime().isBefore(Constant.ZERODHA_SQUARE_OFF_TIME)) {
				cancel(trade, bar);
				return;
			}
		}
		if(TradeValidity.IOC.equals(trade.getValidity())) {
			throw new UnsupportedOperationException();
		} else if(TradeValidity.DAY.equals(trade.getValidity())) {
			if(bar.getBeginTime().toLocalDate().isAfter(trade.getTimestamp().toLocalDate())) {
				cancel(trade, bar);
				return;
			}
		}
		
		if(TradeLimitType.LIMIT.equals(trade.getLimitType())) {
			if(TradeType.BUY.equals(trade.getType())) {
				if(bar.getLowPrice().doubleValue() < trade.getPrice()) {
					trigger(trade, bar);
					return;
				}
			} else if(TradeType.SELL.equals(trade.getType())) {
				if(bar.getHighPrice().doubleValue() > trade.getPrice()) {
					trigger(trade, bar);
					return;
				}
			}
		} else if(TradeLimitType.SL.equals(trade.getLimitType())) {
			if(TradeType.BUY.equals(trade.getType())) {
				if(bar.getHighPrice().doubleValue() > trade.getTriggerPrice()) {
					if(bar.getLowPrice().doubleValue() < trade.getPrice()) {
						trigger(trade, bar);
						return;
					}
				}
			} else if(TradeType.SELL.equals(trade.getType())) {
				if(bar.getLowPrice().doubleValue() < trade.getTriggerPrice()) {
					if(bar.getHighPrice().doubleValue() > trade.getPrice()) {
						trigger(trade, bar);
						return;
					}
				}
			}
		} else if(TradeLimitType.SLM.equals(trade.getLimitType())) {
			if(TradeType.BUY.equals(trade.getType())) {
				if(bar.getHighPrice().doubleValue() > trade.getTriggerPrice()) {
					trigger(trade, bar);
					return;
				}
			} else if(TradeType.SELL.equals(trade.getType())) {
				if(bar.getLowPrice().doubleValue() < trade.getTriggerPrice()) {
					trigger(trade, bar);
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
	
	private void trigger(Trade trade, Bar bar) {
		if(TradeType.SELL.equals(trade.getType())) {
			final int onBalanceQuantity = onBalanceQuantities.computeIfAbsent(trade.getScrip(), key -> 0);
			if(onBalanceQuantity >= trade.getQuantity()) {
				complete(trade, bar, resolveExecutionPrice(trade, bar));
			} else {
				final double executionPrice = resolveExecutionPrice(trade, bar);
				final double requiredMargin = (trade.getQuantity() - onBalanceQuantity) * executionPrice;
				if(portfolio.getAvailableMargin() >= requiredMargin) {
					complete(trade, bar, executionPrice);
				} else {
					reject(trade, bar);
				}
			}
		} else {
			final double executionPrice = resolveExecutionPrice(trade, bar);
			final double requiredMargin = executionPrice * trade.getQuantity();
			if(portfolio.getAvailableMargin() >= requiredMargin) {
				complete(trade, bar, executionPrice);
			} else {
				reject(trade, bar);
			}
		}
	}
	
	private void complete(Trade trade, Bar bar, double price) {
		trade.setAveragePrice(price);
		trade.setStatus(TradeStatus.COMPLETE);
		trade.setFilledQuantity(trade.getQuantity());
		updateOnBalanceQuantity(trade);
		updateAvailableMargin(trade, bar);
	}
	
	private void cancel(Trade trade, Bar bar) {
		trade.setStatus(TradeStatus.CANCELLED);
		updateAvailableMargin(trade, bar);
	}

	private void reject(Trade trade, Bar bar) {
		trade.setStatus(TradeStatus.REJECTED);
		updateAvailableMargin(trade, bar);
	}
	
	private void updateOnBalanceQuantity(Trade trade) {
		final int multiple = TradeType.BUY.equals(trade.getType()) ? 1 : -1;
		final int onBalanceQuantity = onBalanceQuantities.computeIfAbsent(trade.getScrip(), key -> 0);
		onBalanceQuantities.put(trade.getScrip(), onBalanceQuantity + multiple * trade.getFilledQuantity());
	}
	
	private void updateAvailableMargin(Trade trade, Bar bar) {
		int multiple = 1;
		if(TradeType.BUY.equals(trade.getType())) {
			if(TradeStatus.PENDING.equals(trade.getStatus())) {
				multiple = -1;
			} else if(TradeStatus.CANCELLED.equals(trade.getStatus())) {
				multiple = 1;
			}
		} else if(TradeType.SELL.equals(trade.getType())) {
			if(TradeStatus.PENDING.equals(trade.getStatus())) {
				multiple = 1;
			} else if(TradeStatus.CANCELLED.equals(trade.getStatus())) {
				multiple = -1;
			}
		}
		final double executionPrice = resolveExecutionPrice(trade, bar);
		final double effectOnMargin = multiple * executionPrice;
		final double availableMargin = portfolio.getAvailableMargin();
		portfolio.setAvailableMargin(availableMargin + effectOnMargin);
	}
	
}

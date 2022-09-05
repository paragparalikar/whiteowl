package com.whiteowl.core.position;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.ta4j.core.Bar;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Entity
@NoArgsConstructor
@Table(name = "position", indexes = {
		@Index(columnList = "status"),
		@Index(columnList = "tradingStrategyId")
	})
@EqualsAndHashCode(of = {"id", "scrip", "portfolio", "type", "tradingStrategyId"})
public class Position {

	@Id
	@GeneratedValue
	private Long id;
	
	@Valid
	@NotNull @NonNull
	@ManyToOne(optional = false)
	private Scrip scrip;
	
	@Valid
	@NotNull @NonNull
	@ManyToOne(optional = false)
	private Portfolio portfolio;
	
	@NotNull @NonNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private TradeType type;
	
	@NotNull @NonNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PositionStatus status;
	
	@NotNull @NonNull
	@Column(nullable = false, updatable = false)
	private String tradingStrategyId;
	
	private Double targetPrice;
	private Double initialStopLossPrice;
	private Double trailingStopLossPrice;
	
	@NotEmpty
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "position")
	private List<@Valid Trade> entryTrades = new ArrayList<>();
	
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "position")
	private List<@Valid Trade> exitTrades = new ArrayList<>();
	
	public Position withPortfolio(Portfolio portfolio) {
		final Position position = new Position();
		position.setPortfolio(portfolio);
		position.setScrip(scrip);
		position.setType(type);
		position.setStatus(status);
		position.setTradingStrategyId(tradingStrategyId);
		entryTrades.stream()
			.map(trade -> trade.withPosition(position))
			.forEach(position.getEntryTrades()::add);
		exitTrades.stream()
			.map(trade -> trade.withPosition(position))
			.forEach(position.getExitTrades()::add);
		return position;
	}
	
	public Stream<Trade> allTrades() {
		return Stream.concat(entryTrades.stream(), exitTrades.stream());
	}
	
	public boolean shouldClose() {
		return 0 < getEntryQuantity() && getExitQuantity() == getEntryQuantity();
	}
	
	public boolean isClosed() {
		return PositionStatus.CLOSED.equals(status);
	}
	
	public boolean isOpen() {
		return !isClosed();
	}
	
	public boolean isInitialStopHit(Bar bar) {
		return null != initialStopLossPrice && 
				((TradeType.BUY.equals(type) && (getAverageEntryPrice() - initialStopLossPrice) >= bar.getLowPrice().doubleValue()) ||
				(TradeType.SELL.equals(type) && (getAverageEntryPrice() + initialStopLossPrice) <= bar.getHighPrice().doubleValue()));
	}
	
	public boolean isTargetHit(Bar bar) {
		return null != targetPrice &&
				((TradeType.BUY.equals(type) && (getAverageEntryPrice() + targetPrice) <= bar.getHighPrice().doubleValue()) ||
				(TradeType.SELL.equals(type) && (getAverageEntryPrice() - targetPrice) >= bar.getLowPrice().doubleValue()));
	}
	
	public boolean isTrailingStopHit(Bar bar) {
		return null != trailingStopLossPrice &&
				((TradeType.BUY.equals(type) && (getAverageEntryPrice() - trailingStopLossPrice) >= bar.getClosePrice().doubleValue()) ||
				(TradeType.SELL.equals(type) && (getAverageEntryPrice() + trailingStopLossPrice) <= bar.getClosePrice().doubleValue()));
	}
	
	public Optional<LocalDateTime> getEntryTimestamp() {
		return entryTrades.stream()
				.map(Trade::getExchangeTimestamp)
				.filter(Objects::nonNull)
				.min(Comparator.naturalOrder());
	}
	
	public int getEntryQuantity() {
		return entryTrades.stream()
				.filter(trade -> TradeStatus.COMPLETE.equals(trade.getStatus()))
				.collect(Collectors.summingInt(Trade::getQuantity));
	}
	
	public int getExitQuantity() {
		return exitTrades.stream()
				.filter(trade -> TradeStatus.COMPLETE.equals(trade.getStatus()))
				.collect(Collectors.summingInt(Trade::getQuantity));
	}
	
	public double getAverageEntryPrice() {
		return entryTrades.stream()
				.filter(trade -> TradeStatus.COMPLETE.equals(trade.getStatus()))
				.collect(Collectors.averagingDouble(Trade::getAveragePrice));
	}
	
	public double getAverageExitPrice() {
		return exitTrades.stream()
				.filter(trade -> TradeStatus.COMPLETE.equals(trade.getStatus()))
				.collect(Collectors.averagingDouble(Trade::getAveragePrice));
	}
	
	public double getEntryAmount() {
		return entryTrades.stream()
				.filter(trade -> TradeStatus.COMPLETE.equals(trade.getStatus()))
				.map(trade -> trade.getQuantity() * trade.getAveragePrice())
				.collect(Collectors.summingDouble(Double::doubleValue));
	}
	
	public double getExitAmount() {
		return exitTrades.stream()
				.filter(trade -> TradeStatus.COMPLETE.equals(trade.getStatus()))
				.map(trade -> trade.getQuantity() * trade.getAveragePrice())
				.collect(Collectors.summingDouble(Double::doubleValue));
	}
	
	public double getGrossProfitLoss() {
		if(isClosed()) {
			final double change = getExitAmount() - getEntryAmount();
			return TradeType.BUY.equals(type) ? change : -1 * change;
		}
		return 0;
	}
	
	public double getPercentageGrossProfitLoss() {
		return getGrossProfitLoss() * 100 / getEntryAmount();
	}
	
	public double getGrossProfit() {
		return Math.max(0, getGrossProfitLoss());
	}
	
	public double getPercentageGrossProfit() {
		return getGrossProfit() * 100 / getEntryAmount();
	}
	
	public double getGrossLoss() {
		return Math.min(0, getGrossProfitLoss());
	}
	
	public double getPercentageGrossLoss() {
		return getGrossLoss() * 100 / getEntryAmount();
	}
	
}

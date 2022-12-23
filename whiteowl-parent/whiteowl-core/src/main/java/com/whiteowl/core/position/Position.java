package com.whiteowl.core.position;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
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

import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.position.stateMachine.PositionEntityListener;
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
		@Index(columnList = "tradingStrategyConfigId")
	})
@EntityListeners(PositionEntityListener.class)
@EqualsAndHashCode(of = {"id", "scrip", "portfolio", "tradingStrategyConfigId"})
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
	@Column(nullable = false)
	private PositionStatus status = PositionStatus.NEW;
	
	@NotNull @NonNull
	@Column(nullable = false, updatable = false)
	private String tradingStrategyConfigId;
	
	@NotEmpty
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<@Valid Trade> entryTrades = new HashSet<>();
	
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<@Valid Trade> exitTrades = new HashSet<>();
	
	public Position withPortfolio(Portfolio portfolio) {
		final Position position = new Position();
		position.setPortfolio(portfolio);
		position.setScrip(scrip);
		position.setStatus(status);
		position.setTradingStrategyConfigId(tradingStrategyConfigId);
		entryTrades.stream().map(Trade::clone).forEach(position.getEntryTrades()::add);
		exitTrades.stream().map(Trade::clone).forEach(position.getExitTrades()::add);
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
	
	public boolean areAllEntryTradesCompleted() {
		return entryTrades.stream()
				.map(Trade::getStatus)
				.allMatch(TradeStatus::isTerminal);
	}
	
	public boolean areAllExitTradesCompleted() {
		return exitTrades.stream()
				.map(Trade::getStatus)
				.allMatch(TradeStatus::isTerminal);
	}
	
	public boolean hasEntryTrade(Scrip scrip) {
		return null != scrip && entryTrades.stream()
				.map(Trade::getScrip)
				.map(Scrip::getCode)
				.anyMatch(Predicate.isEqual(scrip.getCode()));
	}
	
	public boolean hasExitTrade(Scrip scrip) {
		return null != scrip && exitTrades.stream()
				.map(Trade::getScrip)
				.map(Scrip::getCode)
				.anyMatch(Predicate.isEqual(scrip.getCode()));
	}
	
	void updateStatus() {
		if(exitTrades.isEmpty()) {
			if(entryTrades.isEmpty()) {
				status = PositionStatus.NEW;
			} else {
				status = PositionStatus.OPEN;
			} 
		} else {
			if(areAllEntryTradesCompleted()) {
				if(areAllExitTradesCompleted()) {
					status = PositionStatus.CLOSED;
				} else {
					status = PositionStatus.OPEN;
				} 
			} else {
				status = PositionStatus.OPEN;
			}
		}
	}
}

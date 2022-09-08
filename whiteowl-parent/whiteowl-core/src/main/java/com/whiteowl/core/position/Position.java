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
		@Index(columnList = "tradingStrategyConfigId")
	})
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
	private Long tradingStrategyConfigId;
	
	@NotEmpty
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "position")
	private List<@Valid Trade> entryTrades = new ArrayList<>();
	
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "position")
	private List<@Valid Trade> exitTrades = new ArrayList<>();
	
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
	
}

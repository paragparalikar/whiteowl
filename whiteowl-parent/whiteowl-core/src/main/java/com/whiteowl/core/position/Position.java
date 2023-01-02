package com.whiteowl.core.position;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.ta4j.core.Trade.TradeType;

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
@EntityListeners({AuditingEntityListener.class, PositionEntityListener.class})
@EqualsAndHashCode(of = {"id", "scrip", "portfolio", "tradingStrategyConfigId"})
public class Position {

	@Id
	@GeneratedValue
	private Long id;
	
	@Valid
	@NotNull @NonNull
	@ManyToOne(optional = false)
	@Column(nullable = false, updatable = false)
	private Scrip scrip;
	
	@NotBlank @NonNull
	@Column(nullable = false, updatable = false)
	private String tradingStrategyConfigId;
	
	@Valid
	@NotNull @NonNull
	@ManyToOne(optional = false)
	@Column(nullable = false, updatable = false)
	private Portfolio portfolio;
	
	@NotNull @NonNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PositionStatus status = PositionStatus.NEW;
	
	@NotEmpty
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<@Valid Trade> entryTrades = new HashSet<>();
	
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<@Valid Trade> exitTrades = new HashSet<>();
	
	@CreatedDate
	private LocalDateTime createdDate;
	
	@LastModifiedDate
	private LocalDateTime lastModifiedDate;
	
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
	
	public double getProfitLossAmount() {
		return getAmount(Stream.concat(entryTrades.stream(), exitTrades.stream()), TradeType.SELL)
				- getAmount(Stream.concat(entryTrades.stream(), exitTrades.stream()), TradeType.BUY);
	}
	
	private double getAmount(Stream<Trade> trades, TradeType tradeType) {
		return trades
				.filter(trade -> tradeType.equals(trade.getType()))
				.map(Trade::getAmount)
				.collect(Collectors.summingDouble(Double::doubleValue));
	}
	
	public double getEntryAmount() {
		return entryTrades.stream()
				.map(Trade::getAmount)
				.collect(Collectors.summingDouble(Double::doubleValue));
	}
	
	public double getAverageEntryPrice() {
		return getEntryAmount() / entryTrades.stream().map(Trade::getFilledQuantity)
				.collect(Collectors.summingInt(Integer::intValue));
	}
	
	@PreUpdate
	@PrePersist
	public void updateStatus() {
		if(exitTrades.isEmpty()) {
			if(entryTrades.isEmpty()) {
				status = PositionStatus.NEW;
			} else {
				status = PositionStatus.OPEN;
			} 
		} else {
			status = PositionStatus.OPEN;
			if(entryTrades.stream().map(Trade::getStatus).allMatch(TradeStatus::isTerminal)) {
				if(exitTrades.stream().map(Trade::getStatus).allMatch(TradeStatus::isTerminal)) {
					final Map<Scrip, Integer> buyQuantities = Stream.concat(entryTrades.stream(), exitTrades.stream())
							.filter(trade -> TradeType.BUY.equals(trade.getType()))
							.filter(trade -> TradeStatus.COMPLETE.equals(trade.getStatus()))
							.collect(Collectors.groupingBy(Trade::getScrip, Collectors.summingInt(Trade::getFilledQuantity)));
					final Map<Scrip, Integer> sellQuantities = Stream.concat(entryTrades.stream(), exitTrades.stream())
							.filter(trade -> TradeType.SELL.equals(trade.getType()))
							.filter(trade -> TradeStatus.COMPLETE.equals(trade.getStatus()))
							.collect(Collectors.groupingBy(Trade::getScrip, Collectors.summingInt(Trade::getFilledQuantity)));
					if(buyQuantities.equals(sellQuantities)) {
						status = PositionStatus.CLOSED;
					}
				}
			}
		}
	}

}

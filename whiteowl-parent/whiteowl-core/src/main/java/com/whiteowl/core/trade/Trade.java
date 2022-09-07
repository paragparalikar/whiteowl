package com.whiteowl.core.trade;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.scrip.Scrip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "trade", indexes = {
	@Index(columnList = "status"),
	@Index(columnList = "scrip_code")
})
public class Trade {

	@Id
	@GeneratedValue
	private Long id;
	private String brokerTradeId;
	private String exchangeTradeId;
	
	@Positive private double targetPrice;
	@Positive private double stopLossPrice;
	@PositiveOrZero private double price;
	@PositiveOrZero private double triggerPrice;
	@PositiveOrZero private double averagePrice;
	
	@PositiveOrZero private int quantity;
	@PositiveOrZero private int pendingQuantity;
	@PositiveOrZero private int filledQuantity;
	@PositiveOrZero private int disclosedQuantity;
	
	private String statusMessage;
	private LocalDateTime timestamp;
	private LocalDateTime exchangeTimestamp;
	
	@Valid
	@NonNull @NotNull
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	private Scrip scrip;
	
	@NonNull @NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private TradeType type;
	
	@NonNull @NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private TradeLimitType limitType;
	
	@NonNull @NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private TradeVariety variety;
	
	@NonNull @NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private TradeProduct product;
	
	@NonNull @NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private TradeValidity validity;
	
	@NonNull @NotNull
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private TradeStatus status;
	
	public Trade clone() {
		return Trade.builder()
				.id(id)
				.brokerTradeId(brokerTradeId)
				.exchangeTradeId(exchangeTradeId)
				.scrip(scrip)
				.type(type)
				.limitType(limitType)
				.variety(variety)
				.validity(validity)
				.product(product)
				.status(status)
				.timestamp(timestamp)
				.exchangeTimestamp(exchangeTimestamp)
				.price(price)
				.targetPrice(targetPrice)
				.stopLossPrice(stopLossPrice)
				.triggerPrice(triggerPrice)
				.averagePrice(averagePrice)
				.quantity(quantity)
				.pendingQuantity(pendingQuantity)
				.filledQuantity(filledQuantity)
				.disclosedQuantity(disclosedQuantity)
				.build();
	}
	
	public Trade complement() {
		return Trade.builder()
				.scrip(scrip)
				.type(type.complementType())
				.limitType(limitType)
				.variety(variety)
				.validity(validity)
				.product(product)
				.status(TradeStatus.NEW)
				.quantity(filledQuantity)
				.build();
	}
}

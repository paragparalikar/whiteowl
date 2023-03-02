package com.whiteowl.core.trade;

import java.time.LocalDateTime;

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
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.scrip.Scrip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(of = {"id", "brokerTradeId", "scrip", "type", "limitType", 
		"variety", "product", "validity", "quantity", "price", "triggerPrice"})
public class Trade {

	@Id
	@GeneratedValue
	private Long id;
	private String brokerTradeId;
	private String exchangeTradeId;
	
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
	
	private Double targetPrice;
	
	private Double stopLossPrice;
	
	@NonNull @NotNull
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private TradeStatus status;
	
	@CreatedDate
	private LocalDateTime createdDate;
	
	@LastModifiedDate
	private LocalDateTime lastModifiedDate;
	
	public void copy(Trade trade) {
		this.id = trade.getId();
		this.brokerTradeId = trade.getBrokerTradeId();
		this.exchangeTradeId = trade.getExchangeTradeId();
		this.scrip = trade.getScrip();
		this.type = trade.getType();
		this.limitType = trade.getLimitType();
		this.variety = trade.getVariety();
		this.validity = trade.getValidity();
		this.product = trade.getProduct();
		this.status = trade.getStatus();
		this.statusMessage = trade.getStatusMessage();
		this.timestamp = trade.getTimestamp();
		this.exchangeTimestamp = trade.getExchangeTimestamp();
		this.price = trade.getPrice();
		this.triggerPrice = trade.getTriggerPrice();
		this.averagePrice = trade.getAveragePrice();
		this.quantity = trade.getQuantity();
		this.pendingQuantity = trade.getPendingQuantity();
		this.filledQuantity = trade.getFilledQuantity();
		this.disclosedQuantity = trade.getDisclosedQuantity();
	}
	
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
	
	public double getAmount() {
		return averagePrice * filledQuantity;
	}
	
}

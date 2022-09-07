package com.whiteowl.core.trade;

import java.time.ZonedDateTime;

import org.ta4j.core.Trade.TradeType;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.whiteowl.core.common.ZonedDateTimeDynamoDBTypeConverter;
import com.whiteowl.core.scrip.Exchange;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@DynamoDBDocument
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Trade {

	private String scripCode;
	private String brokerTradeId;
	private String exchangeTradeId;
	private String tradingStrategyConfigId;
	private String statusMessage;
	private double stopLossPrice;
	private double price;
	private double triggerPrice;
	private double averagePrice;
	private int quantity;
	private int pendingQuantity;
	private int filledQuantity;
	private int disclosedQuantity;
	
	@DynamoDBTypeConvertedEnum private TradeType type;
	@DynamoDBTypeConvertedEnum private TradeLimitType limitType;
	@DynamoDBTypeConvertedEnum private TradeVariety variety;
	@DynamoDBTypeConvertedEnum private TradeProduct product;
	@DynamoDBTypeConvertedEnum private TradeValidity validity;
	@DynamoDBTypeConvertedEnum private TradeStatus status;
	@DynamoDBTypeConvertedEnum private Exchange exchange;

	@DynamoDBTypeConverted(converter = ZonedDateTimeDynamoDBTypeConverter.class)
	private ZonedDateTime timestamp;
	
	@DynamoDBTypeConverted(converter = ZonedDateTimeDynamoDBTypeConverter.class)
	private ZonedDateTime exchangeTimestamp;
	
	public Trade clone() {
		return Trade.builder()
				.type(type)
				.scripCode(scripCode)
				.brokerTradeId(brokerTradeId)
				.exchangeTradeId(exchangeTradeId)
				.tradingStrategyConfigId(tradingStrategyConfigId)
				.limitType(limitType)
				.variety(variety)
				.validity(validity)
				.product(product)
				.status(status)
				.timestamp(timestamp)
				.exchangeTimestamp(exchangeTimestamp)
				.price(price)
				.stopLossPrice(stopLossPrice)
				.triggerPrice(triggerPrice)
				.averagePrice(averagePrice)
				.quantity(quantity)
				.pendingQuantity(pendingQuantity)
				.filledQuantity(filledQuantity)
				.disclosedQuantity(disclosedQuantity)
				.statusMessage(statusMessage)
				.build();
	}
	
	public Trade complement() {
		return Trade.builder()
				.scripCode(scripCode)
				.tradingStrategyConfigId(tradingStrategyConfigId)
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

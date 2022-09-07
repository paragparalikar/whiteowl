package com.whiteowl.core.bar;

import java.time.ZonedDateTime;

import org.springframework.data.annotation.Id;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DoubleNum;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.whiteowl.core.common.ZonedDateTimeDynamoDBTypeConverter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "bar")
@EqualsAndHashCode(of = {"code", "timeframe", "beginTime"})
public class PersistentBar implements Comparable<PersistentBar> {
	
	@Id
	private PersistentBarId id;

	@DynamoDBHashKey
	private String code;

	@DynamoDBRangeKey
	@DynamoDBTypeConvertedEnum
	private Timeframe timeframe;
	
	@DynamoDBTypeConverted(converter = ZonedDateTimeDynamoDBTypeConverter.class)
	private ZonedDateTime beginTime;
	
	private long trades;
	
	private double open, high, low, close, amount, volume;
	
	public Bar toBar() {
		return BaseBar.builder()
				.endTime(beginTime.plus(timeframe.getDuration()))
				.timePeriod(timeframe.getDuration())
				.openPrice(DoubleNum.valueOf(open))
				.highPrice(DoubleNum.valueOf(high))
				.lowPrice(DoubleNum.valueOf(low))
				.closePrice(DoubleNum.valueOf(close))
				.amount(DoubleNum.valueOf(amount))
				.volume(DoubleNum.valueOf(volume))
				.trades(trades)
				.build();
	}
	
	@Override
	public int compareTo(PersistentBar other) {
		if(null == other) return 1;
		return beginTime.compareTo(other.getBeginTime());
	}
	
}

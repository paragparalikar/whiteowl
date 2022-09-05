package com.whiteowl.core.bar;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Index;
import javax.persistence.Table;

import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DoubleNum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(PersistentBarKey.class)
@EqualsAndHashCode(of = {"code", "timeframe", "beginTime"})
@Table(name = "bar", indexes = @Index(columnList = "code, timeframe, beginTime", unique = true))
public class PersistentBar implements Comparable<PersistentBar> {

	@Id
	@NonNull
	@Column(nullable = false, length = 64)
	private String code;

	@Id
	@NonNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 3)
	private Timeframe timeframe;
	
	@Id
	@NonNull
	@Column(nullable = false, columnDefinition = "TIMESTAMP")
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

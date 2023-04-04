package com.whiteowl.client.kite.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class CandleSeries {
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

	private List<List<Object>> candles;
	private final List<Candle> data = new ArrayList<>();
	
	public List<Candle> getData(){
		if(data.isEmpty() && null != candles) {
			candles.stream().map(this::parse).forEach(data::add);
		}
		return data;
	}
	
	private Candle parse(List<Object> objects) {
		final long oi = objects.size() >= 7 ? ((Number)toDouble(objects.get(6))).longValue() : 0; 
		return Candle.builder()
				.timestamp(ZonedDateTime.parse((String)objects.get(0), formatter))
				.open(toDouble(objects.get(1)))
				.high(toDouble(objects.get(2)))
				.low(toDouble(objects.get(3)))
				.close(toDouble(objects.get(4)))
				.volume(((Number) objects.get(5)).longValue())
				.openInterest(oi)
				.build();
	}
	
	private Double toDouble(Object value) {
		if(null == value) return null;
		if(value instanceof Double) return (Double) value;
		if(value instanceof Integer) return ((Integer) value).doubleValue();
		return null;
	}
	
}

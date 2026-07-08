package com.whiteowl.core.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

public class LocalDateDynamoDBTypeConverter implements DynamoDBTypeConverter<String, LocalDate> {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
	
	@Override
	public String convert(LocalDate object) {
		return null == object ? null : FORMATTER.format(object);
	}

	@Override
	public LocalDate unconvert(String object) {
		return null == object ? null : LocalDate.parse(object, FORMATTER);
	}

}

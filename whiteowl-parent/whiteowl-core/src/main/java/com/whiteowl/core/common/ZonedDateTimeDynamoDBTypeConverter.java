package com.whiteowl.core.common;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

public class ZonedDateTimeDynamoDBTypeConverter implements DynamoDBTypeConverter<String, ZonedDateTime> {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

	@Override
	public String convert(ZonedDateTime object) {
		return null == object ? null : FORMATTER.format(object);
	}

	@Override
	public ZonedDateTime unconvert(String object) {
		return null == object ? null : ZonedDateTime.parse(object, FORMATTER);
	}


}

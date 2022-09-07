package com.whiteowl.core.attribute;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "attribute")
public class Attribute {

	@DynamoDBHashKey
	private String key;
	
	@DynamoDBAttribute
	private String value;
	
	public Integer getInteger() {
		return null == value ? null : Integer.parseInt(value);
	}
	
	public Double getDouble() {
		return null == value ? null : Double.parseDouble(value);
	}
	
	public Long getLong() {
		return null == value ? null : Long.parseLong(value);
	}
	
	public LocalDate getLocalDate() {
		return null == value ? null : LocalDate.parse(value);
	}
	
	public LocalTime getLocalTime() {
		return null == value ? null : LocalTime.parse(value);
	}
	
	public LocalDateTime getLocalDateTime() {
		return null == value ? null : LocalDateTime.parse(value);
	}
}

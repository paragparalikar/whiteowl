package com.whiteowl.core.attribute;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Attribute {

	@Id
	private String key;
	
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

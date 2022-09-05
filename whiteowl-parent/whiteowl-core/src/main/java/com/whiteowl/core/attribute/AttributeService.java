package com.whiteowl.core.attribute;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttributeService {

	private final AttributeRepository attributeRepository;
	
	public Object set(String key, Object value) {
		attributeRepository.save(new Attribute(key, String.valueOf(value)));
		return value;
	}
	
	public Integer getInteger(String key) {
		return attributeRepository.findById(key).map(Attribute::getInteger).orElse(null);
	}
	
	public Double getDouble(String key) {
		return attributeRepository.findById(key).map(Attribute::getDouble).orElse(null);
	}
	
	public Long getLong(String key) {
		return attributeRepository.findById(key).map(Attribute::getLong).orElse(null);
	}
	
	public LocalDate getLocalDate(String key) {
		return attributeRepository.findById(key).map(Attribute::getLocalDate).orElse(null);
	}
	
	public LocalTime getLocalTime(String key) {
		return attributeRepository.findById(key).map(Attribute::getLocalTime).orElse(null);
	}
	
	public LocalDateTime getLocalDateTime(String key) {
		return attributeRepository.findById(key).map(Attribute::getLocalDateTime).orElse(null);
	}
}

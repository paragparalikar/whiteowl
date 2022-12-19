package com.whiteowl.client.kite.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Response<T> {

	private T data;
	
	private String status;
	
	private String message;
	
	@JsonProperty("error_type")
	private String errorType;
	
}

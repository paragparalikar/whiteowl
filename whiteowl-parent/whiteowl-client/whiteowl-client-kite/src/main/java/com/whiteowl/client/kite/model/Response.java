package com.whiteowl.client.kite.model;

import lombok.Data;

@Data
public class Response<T> {

	private T data;
	
	private String status;
	
	private String message;
	
	private String errorType;
	
}

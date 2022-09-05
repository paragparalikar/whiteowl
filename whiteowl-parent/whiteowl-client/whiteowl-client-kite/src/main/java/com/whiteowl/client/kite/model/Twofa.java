package com.whiteowl.client.kite.model;

import lombok.Data;

@Data
public class Twofa {

	private String userId;
	private String requestId;
	private String twofaType;
	private String twofaStatus;
	
}

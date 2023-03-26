package com.whiteowl.client.kite.model;

import lombok.Data;

@Data
public class Twofa {

	private String userId;
	private String requestId;
	private String twofaType;
	private String[] twofaTypes;
	private String twofaStatus;
	private boolean captcha;
	private boolean locked;
	private Profile profile;
	
}

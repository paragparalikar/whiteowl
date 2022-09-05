package com.whiteowl.client.kite;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KiteCredentials {

	private String pin;
	private String username;
	private String password;
	
}

package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.KiteCredentials;
import com.whiteowl.core.portfolio.Credentials;

public class CredentialsMapper {

	public KiteCredentials toKiteCredentials(Credentials credentials) {
		return KiteCredentials.builder()
				.username(credentials.getUsername())
				.password(credentials.getPassword())
				.pin(credentials.getPin())
				.build();
	}
	
}

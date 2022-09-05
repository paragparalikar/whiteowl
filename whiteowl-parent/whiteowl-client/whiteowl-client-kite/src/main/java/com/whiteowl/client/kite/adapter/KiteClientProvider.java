package com.whiteowl.client.kite.adapter;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.whiteowl.client.kite.KiteClient;
import com.whiteowl.client.kite.KiteConnectApi;
import com.whiteowl.client.kite.KiteCredentials;
import com.whiteowl.client.kite.KiteSession;
import com.whiteowl.client.kite.ResilientKiteClient;

@Component
public class KiteClientProvider {

	@Cacheable
	public KiteConnectApi getClient(KiteCredentials credentials) {
		final KiteSession kiteSession = new KiteSession(credentials);
		final KiteClient kiteClient = new KiteClient(kiteSession);
		return new ResilientKiteClient(kiteClient);
	}
	
}

package com.whiteowl.client.kite.adapter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.whiteowl.client.kite.KiteClient;
import com.whiteowl.client.kite.KiteConnectApi;
import com.whiteowl.client.kite.KiteCredentials;
import com.whiteowl.client.kite.KiteSession;
import com.whiteowl.client.kite.KiteResilientClient;

@Component
public class KiteClientProvider {

	private final Map<KiteCredentials, KiteConnectApi> cache = new HashMap<>();
	
	public KiteConnectApi getClient(KiteCredentials credentials) {
		return cache.computeIfAbsent(credentials, key -> {
			final KiteSession kiteSession = new KiteSession(credentials);
			final KiteClient kiteClient = new KiteClient(kiteSession);
			return new KiteResilientClient(kiteClient);
		});
	}
	
}

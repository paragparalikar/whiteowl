package com.whiteowl.client.kite;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public class KiteClient implements KiteConnectApi {
	
	@Delegate private final KiteHttpClient kiteHttpClient;
	@Delegate private final KiteWebSocketClient kiteWebSocketClient;
	
	public KiteClient(@NonNull final KiteSession session) {
		this.kiteHttpClient = new KiteHttpClient(session);
		this.kiteWebSocketClient = new KiteWebSocketClient(session);
	}
	
}

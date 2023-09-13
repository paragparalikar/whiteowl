package com.whiteowl.client.kite;

import com.whiteowl.client.kite.adapter.KiteDataProviderCredentials;

public class KiteClientTest {

	public static void main(String[] args) {
		final KiteDataProviderCredentials dataProviderCredentials = new KiteDataProviderCredentials();
		dataProviderCredentials.setPin("TFGKP2YOIGOLEFBHHLJ7QQ6PBOKXARFL");
		dataProviderCredentials.setUsername("RP3497");
		dataProviderCredentials.setPassword("Dark@Horse5");
		dataProviderCredentials.setEncrypted(false);
		final KiteCredentials credentials = dataProviderCredentials.toKiteCredentials();
		final KiteSession session = new KiteSession(credentials);
		final KiteConnectApi api = new KiteResilientClient(new KiteClient(session));
		api.getOrders().forEach(System.out::println);
	}
}

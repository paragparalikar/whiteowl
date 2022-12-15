package com.whiteowl.client.kite;

import java.time.ZonedDateTime;

import com.whiteowl.client.kite.adapter.KiteDataProviderCredentials;
import com.whiteowl.client.kite.model.CandleSeries;

public class KiteClientTest {

	public static void main(String[] args) {
		final KiteDataProviderCredentials dataProviderCredentials = new KiteDataProviderCredentials();
		dataProviderCredentials.setPin("TFGKP2YOIGOLEFBHHLJ7QQ6PBOKXARFL");
		dataProviderCredentials.setUsername("RP3497");
		dataProviderCredentials.setPassword("Dark@Horse5");
		dataProviderCredentials.setEncrypted(false);
		final KiteCredentials credentials = dataProviderCredentials.toKiteCredentials();
		final KiteSession session = new KiteSession(credentials);
		final KiteConnectApi api = new KiteClient(session);
		final KiteConnectApi resilientApi = new ResilientKiteClient(api);
		final CandleSeries series = api.getData(738561L, "day", ZonedDateTime.now().minusYears(1), ZonedDateTime.now());
		series.getData().forEach(System.out::println);
	}
}

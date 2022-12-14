package com.whiteowl.client.kite;

import java.time.ZonedDateTime;

import com.whiteowl.client.kite.adapter.KiteDataProviderCredentials;
import com.whiteowl.client.kite.model.CandleSeries;

public class KiteClientTest {

	public static void main(String[] args) {
		System.setProperty("whiteowl.crypto.key", "UXytRK5s40Sett3rj+h2dA==");
		final KiteDataProviderCredentials dataProviderCredentials = new KiteDataProviderCredentials();
		dataProviderCredentials.setPin("cjDp7WCtTW1lcKntKNivqnvYAol9u7aWkhQ22DbL/ONVSwDtU69sTU8kZMOL10Ku");
		dataProviderCredentials.setUsername("Kr8iquq+IQuUtHhn1xMeIQ==");
		dataProviderCredentials.setPassword("u1+iPAl+2lr152hzHgWlWA==");
		final KiteCredentials credentials = dataProviderCredentials.toKiteCredentials();
		final KiteSession session = new KiteSession(credentials);
		final KiteConnectApi api = new KiteClient(session);
		final KiteConnectApi resilientApi = new ResilientKiteClient(api);
		final CandleSeries series = resilientApi.getData(738561L, "day", ZonedDateTime.now().minusYears(1), ZonedDateTime.now());
		series.getData().forEach(System.out::println);
	}
}

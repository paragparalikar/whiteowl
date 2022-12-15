package com.whiteowl.client.kite.request;

import static com.whiteowl.client.kite.KiteConstant.FORMATTER;
import static com.whiteowl.client.kite.KiteConstant.TIMEOUT;
import static com.whiteowl.client.kite.KiteConstant.URL_BARS;
import static com.whiteowl.client.kite.KiteConstant.URL_BASE;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.javalite.http.Get;
import org.javalite.http.Http;

import com.whiteowl.client.kite.KiteSession;

import lombok.NonNull;

public class HistoricalDataRequest extends Get {
	
	private static String buildUrl(long instrumentToken, String interval, 
			ZonedDateTime from, ZonedDateTime to, @NonNull final KiteSession kiteSession) {
		final String url = String.join("/", URL_BASE + URL_BARS, String.valueOf(instrumentToken), interval);
		final Map<String, String> queryParams = new HashMap<>();
		queryParams.put("oi", "1");		
		queryParams.put("to", FORMATTER.format(to));
		queryParams.put("from", FORMATTER.format(from));
		queryParams.put("user_id", kiteSession.getCredentials().getUsername());
		return url + "?" + Http.map2URLEncoded(queryParams);
	}

	public HistoricalDataRequest(long instrumentToken, String interval, 
			ZonedDateTime from, ZonedDateTime to, @NonNull final KiteSession kiteSession) {
		super(buildUrl(instrumentToken, interval, from, to, kiteSession), TIMEOUT, TIMEOUT);
	}

}

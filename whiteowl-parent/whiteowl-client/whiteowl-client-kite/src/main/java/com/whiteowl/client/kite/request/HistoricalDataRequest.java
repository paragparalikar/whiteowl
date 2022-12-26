package com.whiteowl.client.kite.request;

import static com.whiteowl.client.kite.KiteConstant.TIMEOUT;
import static com.whiteowl.client.kite.KiteConstant.URL_BARS;
import static com.whiteowl.client.kite.KiteConstant.URL_BASE;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.javalite.http.Get;
import org.javalite.http.Http;

import com.whiteowl.client.kite.KiteConstant;
import com.whiteowl.client.kite.KiteSession;

import lombok.NonNull;

public class HistoricalDataRequest extends Get {
	
	private static String buildUrl(long instrumentToken, String interval, 
			ZonedDateTime from, ZonedDateTime to, @NonNull final KiteSession kiteSession) {
		final String url = String.join("/", URL_BASE + URL_BARS, String.valueOf(instrumentToken), interval);
		final Map<String, String> queryParams = new HashMap<>();
		queryParams.put("oi", "1");		
		queryParams.put("to", KiteConstant.FORMATTER.format(to));
		queryParams.put("from", KiteConstant.FORMATTER.format(from));
		queryParams.put("user_id", kiteSession.getCredentials().getUsername());
		final String result = url + "?" + Http.map2URLEncoded(queryParams);
		return result;
	}

	public HistoricalDataRequest(long instrumentToken, String interval, 
			ZonedDateTime from, ZonedDateTime to, @NonNull final KiteSession kiteSession) {
		super(buildUrl(instrumentToken, interval, from, to, kiteSession), TIMEOUT, TIMEOUT);
		header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
		header("accept", "application/json, text/plain, */*");
		header("accept-encoding", "gzip, deflate, br");
		header("accept-language", "en-US,en;q=0.9");
		header("sec-ch-ua", "`\"Not?A_Brand`\";v=`\"8`\", `\"Chromium`\";v=`\"108`\", `\"Google Chrome`\";v=`\"108`\"");
		header("sec-ch-ua-mobile", "?0");
		header("sec-ch-ua-platform", "`\"Windows`\"");
		header("sec-fetch-dest", "empty");
		header("sec-fetch-mode", "cors");
		header("sec-fetch-site", "same-origin");
	}

}

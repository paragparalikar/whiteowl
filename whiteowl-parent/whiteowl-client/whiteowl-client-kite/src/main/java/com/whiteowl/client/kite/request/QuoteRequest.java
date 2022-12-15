package com.whiteowl.client.kite.request;

import static com.whiteowl.client.kite.KiteConstant.TIMEOUT;
import static com.whiteowl.client.kite.KiteConstant.URL_BASE;
import static com.whiteowl.client.kite.KiteConstant.URL_DASHBOARD;

import java.util.Collection;
import java.util.stream.Collectors;

import org.javalite.http.Get;

import com.whiteowl.client.kite.KiteConstant;
import com.whiteowl.client.kite.KiteSession;
import com.whiteowl.client.kite.model.Instrument;
import com.whiteowl.client.kite.model.KiteQuoteMode;

import lombok.NonNull;

public class QuoteRequest extends Get {
	
	private static String buildUrl(@NonNull final Collection<Instrument> instruments, 
			@NonNull final KiteQuoteMode mode) {
		final StringBuilder urlBuilder = new StringBuilder();
		switch(mode) {
		case FULL: urlBuilder.append(KiteConstant.URL_QUOTE); break;
		case LTP: urlBuilder.append(KiteConstant.URL_QUOTE_LTP); break;
		case OHLC: urlBuilder.append(KiteConstant.URL_QUOTE_OHLC); break;
		}
		final String queryString = instruments.stream()
			.map(instrument -> "i=" + instrument.getExchange().name() + ":" + instrument.getTradingsymbol())
			.collect(Collectors.joining("&"));
		urlBuilder.append("?" + queryString);
		return urlBuilder.toString();
	}

	public QuoteRequest(
			@NonNull final Collection<Instrument> instruments, 
			@NonNull final KiteQuoteMode mode,
			@NonNull final KiteSession kiteSession) {
		super(buildUrl(instruments, mode), TIMEOUT, TIMEOUT);
		header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
		header("accept", "application/json, text/plain, */*");
		header("accept-encoding", "gzip, deflate, br");
		header("accept-language", "en-US,en;q=0.9");
		header("referer", URL_BASE + URL_DASHBOARD);
		header("sec-ch-ua", "`\"Not?A_Brand`\";v=`\"8`\", `\"Chromium`\";v=`\"108`\", `\"Google Chrome`\";v=`\"108`\"");
		header("sec-ch-ua-mobile", "?0");
		header("sec-ch-ua-platform", "`\"Windows`\"");
		header("sec-fetch-dest", "empty");
		header("sec-fetch-mode", "cors");
		header("sec-fetch-site", "same-origin");
		header("x-kite-app-uuid", kiteSession.getUuid().toString());
		header("x-kite-userid", kiteSession.getCredentials().getUsername());
		header("x-kite-version", "3.0.7");
	}

}

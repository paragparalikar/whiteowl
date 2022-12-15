package com.whiteowl.client.kite.request;

import static com.whiteowl.client.kite.KiteConstant.TIMEOUT;
import static com.whiteowl.client.kite.KiteConstant.URL_BASE;
import static com.whiteowl.client.kite.KiteConstant.URL_DASHBOARD;
import static com.whiteowl.client.kite.KiteConstant.URL_MARGIN;

import org.javalite.http.Get;

import com.whiteowl.client.kite.KiteSession;

import lombok.NonNull;

public class MarginRequest extends Get {

	public MarginRequest(@NonNull final KiteSession kiteSession) {
		super(URL_BASE + URL_MARGIN, TIMEOUT, TIMEOUT);
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

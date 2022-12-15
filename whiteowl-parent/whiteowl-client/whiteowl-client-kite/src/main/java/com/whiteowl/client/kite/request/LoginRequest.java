package com.whiteowl.client.kite.request;

import static com.whiteowl.client.kite.KiteConstant.TIMEOUT;
import static com.whiteowl.client.kite.KiteConstant.URL_BASE;
import static com.whiteowl.client.kite.KiteConstant.URL_LOGIN;

import org.javalite.http.Post;

import com.whiteowl.client.kite.KiteCredentials;
import com.whiteowl.client.kite.KiteSession;

import lombok.NonNull;

public class LoginRequest extends Post {

	public LoginRequest(@NonNull final KiteSession kiteSession) {
		super(URL_BASE + URL_LOGIN, null, TIMEOUT, TIMEOUT);
		final KiteCredentials kiteCredentials = kiteSession.getCredentials();
		param("user_id", kiteCredentials.getUsername());
		param("password", kiteCredentials.getPassword());
		header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
		header("accept", "application/json, text/plain, */*");
		header("accept-encoding", "gzip, deflate, br");
		header("accept-language", "en-US,en;q=0.9");
		header("origin", URL_BASE);
		header("referer", URL_BASE + "/");
		header("cookie", kiteSession.getCookies());
		header("content-type", "application/x-www-form-urlencoded");
		header("sec-ch-ua", "`\"Not?A_Brand`\";v=`\"8`\", `\"Chromium`\";v=`\"108`\", `\"Google Chrome`\";v=`\"108`\"");
		header("sec-ch-ua-mobile", "?0");
		header("sec-ch-ua-platform", "`\"Windows`\"");
		header("sec-fetch-dest", "empty");
		header("sec-fetch-mode", "cors");
		header("sec-fetch-site", "same-origin");
		header("x-kite-app-uuid", kiteSession.getUuid().toString());
		header("x-kite-userid", kiteCredentials.getUsername());
		header("x-kite-version", "3.0.7");
	}

}

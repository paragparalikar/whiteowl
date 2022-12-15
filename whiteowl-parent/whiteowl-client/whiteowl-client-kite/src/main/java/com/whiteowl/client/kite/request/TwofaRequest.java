package com.whiteowl.client.kite.request;

import static com.whiteowl.client.kite.KiteConstant.TIMEOUT;
import static com.whiteowl.client.kite.KiteConstant.URL_BASE;
import static com.whiteowl.client.kite.KiteConstant.URL_TWOFA;

import java.time.LocalDateTime;

import org.javalite.http.Post;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.whiteowl.client.kite.KiteCredentials;
import com.whiteowl.client.kite.KiteSession;
import com.whiteowl.client.kite.model.Twofa;

import lombok.NonNull;
import lombok.SneakyThrows;

public class TwofaRequest extends Post {

	final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

	@SneakyThrows
	private int getTotp(@NonNull String pin) {
		final LocalDateTime localDateTime = LocalDateTime.now();
		final int secondOfMinute = localDateTime.getSecond();
		if(secondOfMinute >= 27 && secondOfMinute <= 29) Thread.sleep(30 - secondOfMinute);
		if(secondOfMinute >= 57 && secondOfMinute <= 59) Thread.sleep(60 - secondOfMinute);
		return googleAuthenticator.getTotpPassword(pin);
	}
	
	public TwofaRequest(@NonNull final Twofa twofaInfo, @NonNull final KiteSession kiteSession) {
		super(URL_BASE + URL_TWOFA, null, TIMEOUT, TIMEOUT);
		final KiteCredentials kiteCredentials = kiteSession.getCredentials();
		param("user_id", kiteCredentials.getUsername());
		param("request_id", twofaInfo.getRequestId());
		param("twofa_type", twofaInfo.getTwofaType());
		param("skip_session", "");
		param("twofa_value", String.valueOf(getTotp(kiteCredentials.getPin())));
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

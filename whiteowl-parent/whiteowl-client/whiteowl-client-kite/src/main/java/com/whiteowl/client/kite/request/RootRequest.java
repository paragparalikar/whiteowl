package com.whiteowl.client.kite.request;

import org.javalite.http.Get;

import com.whiteowl.client.kite.KiteConstant;

public class RootRequest extends Get {

	public RootRequest() {
		super(KiteConstant.URL_BASE, KiteConstant.TIMEOUT, KiteConstant.TIMEOUT);
		header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
		header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
		header("accept-encoding", "gzip, deflate, br");
		header("accept-language", "en-US,en;q=0.9");
		header("sec-ch-ua", "`\"Not?A_Brand`\";v=`\"8`\", `\"Chromium`\";v=`\"108`\", `\"Google Chrome`\";v=`\"108`\"");
		header("sec-ch-ua-mobile", "?0");
		header("sec-ch-ua-platform", "`\"Windows`\"");
		header("sec-fetch-dest", "document");
		header("sec-fetch-mode", "navigate");
		header("sec-fetch-site", "none");
		header("sec-fetch-user", "?1");
		header("upgrade-insecure-requests", "1");
	}

}

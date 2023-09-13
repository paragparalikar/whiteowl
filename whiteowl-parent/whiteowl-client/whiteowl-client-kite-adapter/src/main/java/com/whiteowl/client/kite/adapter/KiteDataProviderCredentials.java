package com.whiteowl.client.kite.adapter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.whiteowl.client.kite.KiteCredentials;
import com.whiteowl.core.util.Crypto;

import lombok.Data;
import lombok.SneakyThrows;

@Data
@Component
@ConfigurationProperties("whiteowl.data-provider.kite.credentials")
public class KiteDataProviderCredentials {

	private String pin;
	private String username;
	private String password;
	private boolean encrypted;
	
	@SneakyThrows
	public KiteCredentials toKiteCredentials() {
		final String keyKey = "whiteowl.crypto.key";
		final String key = System.getProperty(keyKey, System.getenv(keyKey));
		return KiteCredentials.builder()
		.pin(encrypted ? Crypto.decrypt(pin, key) : pin)
		.username(encrypted ? Crypto.decrypt(username, key) : username)
		.password(encrypted ? Crypto.decrypt(password, key) : password)
		.build();
	}
}

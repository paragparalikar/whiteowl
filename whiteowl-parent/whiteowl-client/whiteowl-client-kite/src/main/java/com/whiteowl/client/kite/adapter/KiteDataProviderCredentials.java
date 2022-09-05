package com.whiteowl.client.kite.adapter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.whiteowl.client.kite.KiteCredentials;
import com.whiteowl.core.util.Crypto;

import lombok.Data;
import lombok.SneakyThrows;

@Data
@Component
@ConfigurationProperties("mongoose.data-provider.kite")
public class KiteDataProviderCredentials {

	private String pin;
	private String username;
	private String password;
	
	@SneakyThrows
	public KiteCredentials toKiteCredentials() {
		final String keyKey = "mongoose.crypto.key";
		final String key = System.getProperty(keyKey, System.getenv(keyKey));
		return KiteCredentials.builder()
		.pin(Crypto.decrypt(pin, key))
		.username(Crypto.decrypt(username, key))
		.password(Crypto.decrypt(password, key))
		.build();
	}
}

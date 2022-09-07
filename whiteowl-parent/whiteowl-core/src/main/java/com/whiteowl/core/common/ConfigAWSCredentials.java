package com.whiteowl.core.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSCredentials;

import lombok.Setter;

@Setter
@Component
@ConfigurationProperties("amazon.aws")
public class ConfigAWSCredentials implements AWSCredentials {

	private String accessKey;
	private String secretKey;
	
	@Override
	public String getAWSAccessKeyId() {
		return accessKey;
	}

	@Override
	public String getAWSSecretKey() {
		return secretKey;
	}

}

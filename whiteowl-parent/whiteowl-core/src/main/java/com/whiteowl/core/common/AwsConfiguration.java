package com.whiteowl.core.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

@Configuration
public class AwsConfiguration {

	@Value("${amazon.dynamodb.endpoint}")
    private String amazonDynamoDBEndpoint;
	
	@Value("${amazon.dynamodb.signing-region}")
	private String amazonDynamoDBSigningRegion;

    @Bean
    public AmazonDynamoDB amazonDynamoDB(AWSCredentials awsCredentials) {
    	final AWSCredentialsProvider awsCrendentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
    	final EndpointConfiguration endpointConfiguration = new EndpointConfiguration(amazonDynamoDBEndpoint, amazonDynamoDBSigningRegion);
        return AmazonDynamoDBClientBuilder
        		.standard()
        		.withCredentials(awsCrendentialsProvider)
        		.withEndpointConfiguration(endpointConfiguration)
        		.build();
    }
}

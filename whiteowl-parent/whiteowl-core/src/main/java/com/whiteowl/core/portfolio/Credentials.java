package com.whiteowl.core.portfolio;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@DynamoDBDocument
@NoArgsConstructor
@AllArgsConstructor
public class Credentials {

	@NonNull
	@NotBlank
	@Size(min = 3, max = 255)
	private String username;
	
	@NonNull
	@NotBlank
	@Size(min = 3, max = 255)
	private String password;
	
	@NonNull
	@NotBlank
	@Size(min = 3, max = 255)
	private String pin;
	
}
	
package com.whiteowl.core.bar;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersistentBarId {

	@DynamoDBHashKey
	private String code;

	@DynamoDBRangeKey
	@DynamoDBTypeConvertedEnum
	private Timeframe timeframe;
	
}

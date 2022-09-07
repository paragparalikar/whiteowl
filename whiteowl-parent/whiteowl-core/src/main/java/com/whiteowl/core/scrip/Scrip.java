package com.whiteowl.core.scrip;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel.DynamoDBAttributeType;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped;
import com.whiteowl.core.common.LocalDateDynamoDBTypeConverter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "scrip")
public class Scrip implements Comparable<Scrip> {

	@NotBlank
	@DynamoDBHashKey
	private String code;
	
	private String name;
	
	@DynamoDBTypeConverted(converter = LocalDateDynamoDBTypeConverter.class)
	private LocalDate expiry;
	
	private double strike;
	
	private double tickSize;
	
	private int lotSize;
	
	@DynamoDBTypeConvertedEnum
	private Segment segment;
	
	@NonNull @NotNull
	@DynamoDBTypeConvertedEnum
	private ScripType type;
	
	@NonNull @NotNull
	@DynamoDBTypeConvertedEnum
	private Exchange exchange;
	
	@DynamoDBTyped(DynamoDBAttributeType.SS)
	private Set<Index> indices;
	
	@Override
	public int compareTo(Scrip other) {
		return Objects.compare(getName(), other.getName(), Comparator.naturalOrder());
	}
}

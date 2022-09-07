package com.whiteowl.core.user;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "user")
public class User implements UserDetails {
	private static final long serialVersionUID = -8229302054396449750L;

	@DynamoDBHashKey
	private String username;
	
	@DynamoDBAttribute
	private String firstName;
	
	@DynamoDBAttribute
	private String lastName;
	
	@DynamoDBAttribute
	private String password;
	
	@DynamoDBIgnore
	public String getDisplayName() {
		return lastName + ", " + firstName;
	}

	@Override
	@DynamoDBIgnore
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.emptyList();
	}

	@Override
	@DynamoDBIgnore
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	@DynamoDBIgnore
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	@DynamoDBIgnore
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	@DynamoDBIgnore
	public boolean isEnabled() {
		return true;
	}
	
}

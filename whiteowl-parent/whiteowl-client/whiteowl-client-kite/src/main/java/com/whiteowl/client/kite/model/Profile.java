package com.whiteowl.client.kite.model;

import java.util.List;

import lombok.Data;

@Data
public class Profile {
	
	private String userId;
	
	private String twofaType;
	
	private String userName;
	
	private String userType;
	
	private String email;
	
	private String phone;
	
	private String broker;
	
	private BankAccount bankAccount;
	
	private List<String> dpIds;
	
	private List<Product> products;
	
	private List<OrderType> orderTypes;
	
	private List<KiteExchange> exchanges;
	
	private String pan;
	
	private String userShortname;
	
	private String avatarUrl;
	
	private List<String> tags;
	
}

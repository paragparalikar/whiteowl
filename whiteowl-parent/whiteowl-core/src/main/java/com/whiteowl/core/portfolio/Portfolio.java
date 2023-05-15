package com.whiteowl.core.portfolio;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import com.whiteowl.core.broker.Broker;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Entity
@NoArgsConstructor
public class Portfolio implements Serializable {
	private static final long serialVersionUID = -4012614641349091811L;

	@Id
	@GeneratedValue
	private Long id;

	@NonNull
	@NotBlank
	@Size(min = 3, max = 255)
	@Column(nullable = false)
	private String name;

	@NonNull
	@NotNull
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private Broker broker = Broker.TEST;

	@Valid
	@NonNull
	@NotNull
	@Embedded
	private Credentials credentials = new Credentials();

	public synchronized Credentials getCredentials() {
		return credentials == null ? credentials = new Credentials() : credentials;
	}

	@PositiveOrZero
	private double maxTradableAmount;

	private double availableMargin;
	
}

package com.whiteowl.core.portfolio;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class Credentials {

	@NonNull
	@NotBlank
	@Size(min = 3, max = 255)
	@Column(nullable = false)
	private String username;
	
	@NonNull
	@NotBlank
	@Size(min = 3, max = 255)
	@Column(nullable = false)
	private String password;
	
	@NonNull
	@NotBlank
	@Size(min = 3, max = 255)
	@Column(nullable = false)
	private String pin;
	
}

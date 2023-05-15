package com.whiteowl.core.scrip;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scrip implements Comparable<Scrip>, Serializable {
	private static final long serialVersionUID = -5356392635225187616L;

	@Id @NotBlank
	private String code;
	private String name;
	private LocalDate expiry;
	private double strike;
	private double tickSize;
	private int lotSize;
	private Segment segment;
	private boolean underlying;
	
	@NonNull @NotNull
	@Enumerated(EnumType.STRING)
	private ScripType type;
	
	@NonNull @NotNull
	@Enumerated(EnumType.STRING)
	private Exchange exchange;
	
	@Builder.Default
	@Enumerated(EnumType.STRING)
	@ElementCollection(fetch = FetchType.EAGER)
	private Set<Index> indices = new HashSet<>();
	
	@Override
	public int compareTo(Scrip other) {
		return Objects.compare(getName(), other.getName(), Comparator.naturalOrder());
	}
	
	public boolean isDerivative() {
		return null != type && type.isDerivative();
	}
	
	public boolean isOption() {
		return null != type && type.isOption();
	}
}

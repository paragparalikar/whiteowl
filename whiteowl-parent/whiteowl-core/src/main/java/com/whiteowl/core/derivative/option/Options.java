package com.whiteowl.core.derivative.option;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.whiteowl.core.scrip.Scrip;

public interface Options {

	public static final DateTimeFormatter EXPIRY_DATE_FORMATTER = DateTimeFormatter.ofPattern("YYMMM");
	
	public static String resolveUnderlyingCode(Scrip scrip) {
		if(!scrip.isDerivative()) return null;
		final LocalDate expiryDate = scrip.getExpiry();
		final String expiryDateText = EXPIRY_DATE_FORMATTER.format(expiryDate).toUpperCase();
		final int offset = scrip.getCode().indexOf(expiryDateText);
		return 0 > offset ? null : scrip.getCode().substring(0, offset);
	}
	
}

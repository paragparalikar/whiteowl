package com.whiteowl.core.scrip;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Segment {

	BCD("BCD"), 
	BCDFUT("BCD-FUT"), 
	BCDOPT("BCD-OPT"), 
	BSE("BSE"), 
	CDSFUT("CDS-FUT"), 
	CDSOPT("CDS-OPT"), 
	INDICES("INDICES"), 
	MCXFUT("MCX-FUT"), 
	MCXOPT("MCX-OPT"), 
	NFOFUT("NFO-FUT"), 
	NFOOPT("NFO-OPT"), 
	NSE("NSE");
	
	public static Segment findByName(String name) {
		return Arrays.stream(Segment.values())
				.filter(segment -> segment.getName().equalsIgnoreCase(name))
				.findFirst().orElse(null);
	}
	
	private final String name;
	
}

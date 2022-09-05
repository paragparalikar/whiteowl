package com.whiteowl.core.quote;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MarketDepth {

	private List<Depth> buy = new ArrayList<>();
	private List<Depth> sell = new ArrayList<>();
	
}

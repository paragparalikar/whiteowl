package com.whiteowl.core.bar;

import java.util.List;

import org.ta4j.core.Bar;

import com.whiteowl.core.bar.query.BarQuery;

public interface BarDataProvider {
	
	List<Bar> getBars(BarQuery barQuery);

}

package com.whiteowl.core.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public interface Constant {
	Num ZERO = DoubleNum.valueOf(0);

	int NSE_START_HOUR = 9;
	int NSE_START_MINUTE = 15;
	int NSE_STOP_HOUR = 15;
	int NSE_STOP_MINUTE = 30;
	
	Path HOME = Paths.get(System.getProperty("user.home"), ".mongoose");
	
}

package com.whiteowl.core.derivative.option;

import static com.whiteowl.core.scrip.ScripType.CE;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import com.whiteowl.core.scrip.ScripType;
import com.whiteowl.core.util.Constant;

import lombok.Builder;
import lombok.Value;

// https://github.com/harshucheri/Java-algo-trading-and-Option-greeks-calculator/blob/main/Black-Scholes-Option-Pricing-Model-master/Black-Scholes-Option-Pricing-Model-master/Code/BSOption.java

@Value
public class Greeks {

	private final double iv, delta, gamma, vega, theta, volga;
	
	@Builder
	public Greeks(double spotPrice, double strikePrice, double spotVolatility, double price, 
			ZonedDateTime timestamp, LocalDate expiry, ScripType type) {
		this(spotPrice, strikePrice, computeTimeToExpiry(timestamp, expiry), 6d, 14d, spotVolatility, price, type);
	}

	private Greeks(double S, double K, double T, double r, double q, double vol, double price, ScripType type) {
		final double dplus = (Math.log(S / K) + (r - q + Math.pow(vol,2) / 2) * T) / (vol * Math.sqrt(T));
	    final double dminus = dplus - vol * Math.sqrt(T);
	    this.delta = computeDelta(dplus, q, T, type);
	    this.vega = computeVega(dplus, S, q, T);
	    this.theta = computeTheta(dplus, dminus, S, K, T, r, q, vol, type);
	    this.gamma = computeGamma(dplus, S, T, q, vol);
	    this.volga = computeVolga(vega, dplus, dminus, vol);
		this.iv = computeIv(price, S, K, T, r, q, type);
	}
	
	private static double computeTimeToExpiry(ZonedDateTime timestamp, LocalDate expiry) {
		final LocalDateTime now = timestamp.toLocalDateTime();
		final LocalDateTime expiryTime = LocalDateTime.of(expiry, Constant.NSE_END_TIME);
		return Duration.between(now, expiryTime).abs().toDays() / 365d;
	}
	
	private double computeDelta(double dplus, double q, double T, ScripType type) {
		return CE.equals(type) ? Math.exp(-q*T)*CND(dplus) : -Math.exp(-q*T)*CND(-dplus);
	}
	
	private double computeVega(double dplus, double S, double q, double T) {
		return S*Math.exp(-q*T)*ND(dplus)*Math.sqrt(T);
	}
	
	private double computeTheta(double dplus, double dminus, double S, double K, double T, double r, double q, double vol, ScripType type) {
		return CE.equals(type) ? 
				-S*ND(dplus)*vol/(2*Math.sqrt(T))-r*K*Math.exp(-r*T)*CND(dminus)+q*S*Math.exp(-q*T)*CND(dplus) :
				-S*ND(dplus)*vol/(2*Math.sqrt(T))+r*K*Math.exp(-r*T)*CND(-dminus)-q*S*Math.exp(-q*T)*CND(-dplus);
	}
	
	private double computeGamma(double dplus, double S, double T, double q, double vol) {
		return Math.exp(-q*T)*ND(dplus)/(S*vol*Math.sqrt(T));
	}
	
	private double computeVolga(double vega, double dplus, double dminus, double vol) {
		return vega*dplus*dminus/vol;
	}

	private double computeIv(double price, double S, double K, double T, double r, double q, ScripType type) {
		int i = 0;
		double epsilon = Math.pow(10,-3);
		double X2 = 0.05;
		double p = computeVolatility(X2, S, K, T, r, q, type) - price;
		while (i < 300 && Math.abs(p) > epsilon) {
			double dplus = (Math.log(S / K) + (r - q + Math.pow(X2, 2) / 2) * T) / (X2 * Math.sqrt(T));
			X2 = X2 - (computeVolatility(X2, S, K, T, r, q, type) - price) / (S * Math.exp(-q * T) * ND(dplus) * Math.sqrt(T));
			p = computeVolatility(X2, S, K, T, r, q, type) - price;
			i++;
		}
		return X2;
	}

	private double computeVolatility(double v, double S, double K, double T, double r, double q, ScripType type) {
		double dplus = (Math.log(S / K) + (r - q + Math.pow(v, 2) / 2) * T) / (v * Math.sqrt(T));
		double dminus = dplus - v * Math.sqrt(T);
		return CE.equals(type) ? 
				S * CND(dplus) * Math.exp(-q * T) - K * CND(dminus) * Math.exp(-r * T) : 
				S * -1 * CND(-dplus) * Math.exp(-q * T) + K * CND(-dminus) * Math.exp(-r * T);
	}

	private static double ND(double x) {
		return 1 / Math.sqrt(2 * Math.PI) * Math.exp(-Math.pow(x, 2) / 2);
	}

	private static double CND(double x) {
		double a1 = 0.31938153;
		double a2 = -0.356563782;
		double a3 = 1.781477937;
		double a4 = -1.821255978;
		double a5 = 1.330274429;
		double L = Math.abs(x);
		double K = 1 / (1 + 0.2316419 * L);
		double res = 1 - 1 / Math.sqrt(2 * Math.PI) * Math.exp(-Math.pow(L, 2) / 2)
				* (a1 * K + a2 * Math.pow(K, 2) + a3 * Math.pow(K, 3) + a4 * Math.pow(K, 4) + a5 * Math.pow(K, 5));
		if (x < 0)
			res = 1 - res;
		return res;
	}

}

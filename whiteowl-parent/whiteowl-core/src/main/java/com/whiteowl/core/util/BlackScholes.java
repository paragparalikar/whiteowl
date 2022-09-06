package com.whiteowl.core.util;

public class BlackScholes {
	
	public static void main(String[] args) {
		BlackScholes one = new BlackScholes(95, 90, .05, .2, 30);
		System.out.println("\t\tCall\t\t\tPut");
		System.out.println("Value\t" + one.valuateCallOption()+"\t\t\t"+one.valuatePutOption());
		System.out.println("Delta\t"+one.getCallDelta() + "\t\t\t" + one.getPutDelta());
		System.out.println("Theta\t"+one.getCallTheta() + "\t\t\t" + one.getPutTheta());
		System.out.println("Gamma\t"+one.getGamma());
		System.out.println("Vega\t"+one.getVega());
	}

	private double S;
	private double K;
	private double r;
	private double v;
	private double tau;
	private double dPF; // discounted probability factor
	private double ePF; // exercise probability factor

	public BlackScholes(double underlyingPrice, double strikePrice, 
			double interestRate, double volatility, double daysToExpiry) {
		this.S = underlyingPrice;
		this.K = strikePrice;
		this.r = interestRate;
		this.v = volatility;
		daysToExpiry = daysToExpiry / 365;
		this.tau = daysToExpiry;
		this.dPF = (Math.log(underlyingPrice / strikePrice) + ((interestRate + (volatility * volatility) / 2)) * daysToExpiry) / (volatility * Math.sqrt(daysToExpiry));
		this.ePF = dPF - (volatility * Math.sqrt(daysToExpiry));

	}

	public double valuatePutOption() {
		return (K * Math.exp(-r * tau) * CNDF(-ePF)) - (S * CNDF(-dPF));
	}

	public double valuateCallOption() {
		return (S * CNDF(dPF)) - (K * Math.exp(-r * tau)) * CNDF(ePF);
	}

	public double getCallDelta() {
		return CNDF(dPF);
	}

	public double getPutDelta() {
		return getCallDelta() - 1;
	}

	public double getCallTheta() {
		double chunk1 = -r * K * Math.exp(-r * tau) * CNDF(ePF);
		double chunk2 = (v * S * PNDF(dPF)) / (2 * Math.sqrt(tau));
		return (chunk1 - chunk2) / 365;
	}

	public double getPutTheta() {
		double chunk1 = r * K * Math.exp(-r * tau) * CNDF(-ePF);
		double chunk2 = (v * S * PNDF(dPF)) / (2 * Math.sqrt(tau));
		return (chunk1 - chunk2) / 365;

	}

	public double getCallRho() {
		return K * tau * Math.exp(-r * tau) * CNDF(ePF) / 100;
	}

	public double getPutRho() {
		return -K * tau * Math.exp(-r * tau) * CNDF(-ePF) / 100;
	}

	public double getGamma() {
		return PNDF(dPF) / (S * v * Math.sqrt(tau));
	}

	public double getVega() {
		return S * Math.sqrt(tau) * PNDF(dPF) / 1000;
	}

	public double PNDF(double x) {
		return (1 / Math.sqrt(2 * Math.PI)) * Math.exp(-(x * x) / 2);
	}

	// Source from:
	// http://www.codeproject.com/Messages/2622967/Re-NORMSDIST-function.aspx
	public double CNDF(double x) {
		int neg = (x < 0d) ? 1 : 0;
		if (neg == 1)
			x *= -1d;

		double k = (1d / (1d + 0.2316419 * x));
		double y = ((((1.330274429 * k - 1.821255978) * k + 1.781477937) * k - 0.356563782) * k + 0.319381530) * k;
		y = 1.0 - 0.398942280401 * Math.exp(-0.5 * x * x) * y;

		return (1d - neg) * y + neg * (1d - y);
	}

}

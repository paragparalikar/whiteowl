package com.whiteowl.core.backtest.rotational;

import java.util.Random;

/**
 * Statistical significance tests for backtest results.
 *
 * <p>Implements three complementary tests to determine whether an observed
 * Sharpe ratio represents real alpha or is an artifact of overfitting:</p>
 * <ol>
 *   <li><b>Deflated Sharpe Ratio</b> (Bailey &amp; Lopez de Prado, 2014) — adjusts
 *       the observed Sharpe for the number of trials tested, plus skewness/kurtosis
 *       of returns.</li>
 *   <li><b>Bootstrap confidence intervals</b> — resample daily returns with replacement
 *       to build a distribution of Sharpe ratios and report percentile bounds.</li>
 *   <li><b>Permutation test</b> — randomly shuffle daily returns to destroy any serial
 *       dependence and measure how often chance alone produces a Sharpe as high as
 *       the observed one.</li>
 * </ol>
 *
 * <p>All methods are static and operate on arrays of daily returns (percentage, not
 * absolute P&amp;L), consistent with {@link RotationalMetrics}.</p>
 *
 * @see <a href="https://ssrn.com/abstract=2460551">Bailey &amp; Lopez de Prado (2014)</a>
 */
public final class StatisticalSignificanceAnalyzer {

    private StatisticalSignificanceAnalyzer() {}

    // ── Deflated Sharpe Ratio ────────────────────────────────────────────

    /**
     * Result of the Deflated Sharpe Ratio computation.
     *
     * @param dsr      the deflated Sharpe ratio (observed SR minus expected max SR,
     *                 normalized by standard error)
     * @param pValue   probability that the observed SR could be produced by chance
     *                 after the given number of trials
     * @param srMax    expected maximum Sharpe ratio from {@code numTrials} zero-skill
     *                 strategies (the "haircut bar")
     */
    public record DeflatedSharpeResult(double dsr, double pValue, double srMax) {}

    /**
     * Compute the Deflated Sharpe Ratio.
     *
     * <p>Adjusts the observed (annualized) Sharpe for the number of independent
     * parameter combinations tested. Uses the Euler-Mascheroni / extreme-value
     * approximation for the expected maximum of {@code numTrials} i.i.d. normal
     * draws (Bailey &amp; Lopez de Prado, 2014, eq. 6).</p>
     *
     * @param dailyReturns array of daily percentage returns
     * @param numTrials    total number of strategy configurations tested (M)
     * @return deflated Sharpe result
     */
    public static DeflatedSharpeResult deflatedSharpe(double[] dailyReturns, int numTrials) {
        int T = dailyReturns.length;
        if (T < 2 || numTrials < 1) {
            return new DeflatedSharpeResult(0, 1, 0);
        }

        double mean = mean(dailyReturns);
        double std = std(dailyReturns, mean);
        double observedSR = (std > 0) ? mean / std * Math.sqrt(252) : 0;

        double skewness = skewness(dailyReturns, mean, std);
        double kurtosis = kurtosis(dailyReturns, mean, std);

        // Expected maximum SR under the null (zero-skill), Bailey & LdP eq. 6
        double srMax;
        if (numTrials <= 1) {
            srMax = 0;
        } else {
            double lnM = Math.log(numTrials);
            double z = Math.sqrt(2 * lnM);
            double eZmax = z - (Math.log(Math.log(numTrials)) + Math.log(4 * Math.PI)) / (2 * z);
            srMax = eZmax * Math.sqrt(1.0 / (T - 1));
        }

        // Standard error of SR with non-normality correction (Lo, 2002)
        double seSR = Math.sqrt(Math.max(
                (1 - skewness * observedSR + ((kurtosis - 1) / 4.0) * observedSR * observedSR)
                        / (T - 1),
                1e-12
        ));

        double dsr = (observedSR - srMax) / seSR;
        double pValue = 1.0 - normCdf(dsr);

        return new DeflatedSharpeResult(dsr, pValue, srMax);
    }

    // ── Bootstrap Confidence Intervals ───────────────────────────────────

    /**
     * Result of bootstrap confidence interval estimation.
     *
     * @param observedSharpe the original Sharpe ratio
     * @param lower          lower bound of the confidence interval
     * @param upper          upper bound of the confidence interval
     * @param confidence     confidence level (e.g. 0.90 for 90% CI)
     * @param medianSharpe   median Sharpe across bootstrap samples
     */
    public record BootstrapResult(double observedSharpe, double lower, double upper,
                                  double confidence, double medianSharpe) {}

    /**
     * Estimate confidence intervals for the Sharpe ratio via bootstrap resampling.
     *
     * <p>Draws {@code iterations} bootstrap samples (with replacement) of the same
     * length as the original daily returns, computes the annualized Sharpe for each
     * sample, and reports the percentile-based confidence interval.</p>
     *
     * @param dailyReturns array of daily percentage returns
     * @param iterations   number of bootstrap iterations (e.g. 10,000)
     * @param confidence   confidence level (e.g. 0.90 for 90% CI)
     * @param seed         random seed for reproducibility
     * @return bootstrap result with CI bounds
     */
    public static BootstrapResult bootstrapSharpe(double[] dailyReturns, int iterations,
                                                  double confidence, long seed) {
        int T = dailyReturns.length;
        if (T < 2) {
            return new BootstrapResult(0, 0, 0, confidence, 0);
        }

        double observedSharpe = annualizedSharpe(dailyReturns);
        double[] bootstrapSharpes = new double[iterations];
        Random rng = new Random(seed);

        double[] sample = new double[T];
        for (int i = 0; i < iterations; i++) {
            // Resample with replacement
            for (int j = 0; j < T; j++) {
                sample[j] = dailyReturns[rng.nextInt(T)];
            }
            bootstrapSharpes[i] = annualizedSharpe(sample);
        }

        java.util.Arrays.sort(bootstrapSharpes);

        double alpha = (1 - confidence) / 2.0;
        int lowerIdx = (int) Math.floor(alpha * iterations);
        int upperIdx = (int) Math.floor((1 - alpha) * iterations);
        lowerIdx = Math.max(0, Math.min(lowerIdx, iterations - 1));
        upperIdx = Math.max(0, Math.min(upperIdx, iterations - 1));

        double medianSharpe = bootstrapSharpes[iterations / 2];

        return new BootstrapResult(observedSharpe,
                bootstrapSharpes[lowerIdx], bootstrapSharpes[upperIdx],
                confidence, medianSharpe);
    }

    // ── Permutation Test ─────────────────────────────────────────────────

    /**
     * Result of the permutation test.
     *
     * @param observedSharpe the original Sharpe ratio
     * @param pValue         fraction of permuted samples with Sharpe >= observed
     * @param iterations     number of permutations performed
     * @param meanNullSharpe mean Sharpe under the null (permuted data)
     * @param maxNullSharpe  maximum Sharpe observed across permutations
     */
    public record PermutationResult(double observedSharpe, double pValue, int iterations,
                                    double meanNullSharpe, double maxNullSharpe) {}

    /**
     * Permutation test for Sharpe ratio significance.
     *
     * <p>Randomly shuffles the daily returns (destroying any temporal structure)
     * and re-computes the Sharpe ratio. The p-value is the fraction of shuffled
     * samples whose Sharpe equals or exceeds the observed Sharpe.</p>
     *
     * <p>Under the null hypothesis (no skill), the order of returns doesn't matter,
     * so the permuted Sharpe should be close to the observed Sharpe. This test is
     * most informative when the strategy's edge comes from timing (serial dependence)
     * rather than pure stock selection. For a long/short strategy with both longs
     * and shorts each day, shuffling daily aggregate returns tests whether the
     * observed Sharpe is consistent with the return distribution.</p>
     *
     * @param dailyReturns array of daily percentage returns
     * @param iterations   number of permutations (e.g. 10,000)
     * @param seed         random seed for reproducibility
     * @return permutation test result
     */
    public static PermutationResult permutationTest(double[] dailyReturns, int iterations,
                                                    long seed) {
        int T = dailyReturns.length;
        if (T < 2) {
            return new PermutationResult(0, 1, iterations, 0, 0);
        }

        double observedSharpe = annualizedSharpe(dailyReturns);

        Random rng = new Random(seed);
        double[] shuffled = new double[T];
        int exceedCount = 0;
        double nullSum = 0;
        double maxNull = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < iterations; i++) {
            // Copy and shuffle (Fisher-Yates)
            System.arraycopy(dailyReturns, 0, shuffled, 0, T);
            for (int j = T - 1; j > 0; j--) {
                int k = rng.nextInt(j + 1);
                double tmp = shuffled[j];
                shuffled[j] = shuffled[k];
                shuffled[k] = tmp;
            }

            double permSharpe = annualizedSharpe(shuffled);
            nullSum += permSharpe;
            if (permSharpe > maxNull) maxNull = permSharpe;
            if (permSharpe >= observedSharpe) exceedCount++;
        }

        double pValue = (double) exceedCount / iterations;
        double meanNull = nullSum / iterations;

        return new PermutationResult(observedSharpe, pValue, iterations, meanNull, maxNull);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Annualized Sharpe ratio from daily returns. */
    static double annualizedSharpe(double[] returns) {
        if (returns.length < 2) return 0;
        double m = mean(returns);
        double s = std(returns, m);
        return (s > 0) ? m / s * Math.sqrt(252) : 0;
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private static double std(double[] values, double mean) {
        double sumSq = 0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / values.length);
    }

    /** Sample skewness (Fisher). */
    private static double skewness(double[] values, double mean, double std) {
        if (std <= 0 || values.length < 3) return 0;
        int n = values.length;
        double sum = 0;
        for (double v : values) {
            double z = (v - mean) / std;
            sum += z * z * z;
        }
        return sum / n;
    }

    /** Sample excess kurtosis + 3 (i.e. raw kurtosis, normal = 3). */
    private static double kurtosis(double[] values, double mean, double std) {
        if (std <= 0 || values.length < 4) return 3;
        int n = values.length;
        double sum = 0;
        for (double v : values) {
            double z = (v - mean) / std;
            sum += z * z * z * z;
        }
        return sum / n;
    }

    /** Standard normal CDF using the error function. */
    private static double normCdf(double x) {
        return 0.5 * (1 + erf(x / Math.sqrt(2)));
    }

    /**
     * Error function approximation (Abramowitz &amp; Stegun, 7.1.26).
     * Maximum error ~1.5e-7.
     */
    private static double erf(double x) {
        // Save the sign of x
        double sign = (x >= 0) ? 1 : -1;
        x = Math.abs(x);

        double t = 1.0 / (1.0 + 0.3275911 * x);
        double y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741)
                * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-x * x);

        return sign * y;
    }
}

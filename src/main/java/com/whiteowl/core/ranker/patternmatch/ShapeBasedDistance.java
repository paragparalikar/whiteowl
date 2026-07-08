package com.whiteowl.core.ranker.patternmatch;

final class ShapeBasedDistance {

    private ShapeBasedDistance() {
    }

    static double compute(float[] series1, float[] series2) {
        float[] norm1 = SeriesTransform.zScoreNormalize(series1);
        float[] norm2 = SeriesTransform.zScoreNormalize(series2);
        double ncc = maxNormalizedCrossCorrelation(norm1, norm2);
        return 1.0 - ncc;
    }

    static double computeDerivative(float[] series1, float[] series2) {
        float[] deriv1 = SeriesTransform.toDerivative(series1);
        float[] deriv2 = SeriesTransform.toDerivative(series2);
        return compute(deriv1, deriv2);
    }

    static double computeMultiChannel(float[][] query, float[][] candidate) {
        int channels = query.length;
        double totalDistance = 0;
        for (int c = 0; c < channels; c++) {
            totalDistance += compute(query[c], candidate[c]);
        }
        return totalDistance / channels;
    }

    private static double maxNormalizedCrossCorrelation(float[] x, float[] y) {
        int n = x.length;
        int m = y.length;
        int fftSize = nextPowerOfTwo((n + m - 1) * 2);
        double[] xr = new double[fftSize];
        double[] xi = new double[fftSize];
        double[] yr = new double[fftSize];
        double[] yi = new double[fftSize];
        for (int i = 0; i < n; i++) xr[i] = x[i];
        for (int i = 0; i < m; i++) yr[i] = y[i];
        fft(xr, xi, false);
        fft(yr, yi, false);
        double[] cr = new double[fftSize];
        double[] ci = new double[fftSize];
        for (int i = 0; i < fftSize; i++) {
            cr[i] = xr[i] * yr[i] + xi[i] * yi[i];
            ci[i] = xi[i] * yr[i] - xr[i] * yi[i];
        }
        fft(cr, ci, true);
        double normX = norm(x);
        double normY = norm(y);
        double denom = normX * normY;
        if (denom == 0) return 0;
        double maxNcc = -Double.MAX_VALUE;
        for (int i = 0; i < fftSize; i++) {
            double ncc = cr[i] / denom;
            if (ncc > maxNcc) maxNcc = ncc;
        }
        return Math.min(maxNcc, 1.0);
    }

    private static double norm(float[] arr) {
        double sum = 0;
        for (float v : arr) {
            sum += (double) v * v;
        }
        return Math.sqrt(sum);
    }

    private static int nextPowerOfTwo(int n) {
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }

    private static void fft(double[] real, double[] imag, boolean inverse) {
        int n = real.length;
        int bits = Integer.numberOfTrailingZeros(n);
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - bits);
            if (j > i) {
                double tempR = real[i]; real[i] = real[j]; real[j] = tempR;
                double tempI = imag[i]; imag[i] = imag[j]; imag[j] = tempI;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double angle = 2 * Math.PI / len * (inverse ? -1 : 1);
            double wR = Math.cos(angle);
            double wI = Math.sin(angle);
            for (int i = 0; i < n; i += len) {
                double curR = 1, curI = 0;
                for (int j = 0; j < len / 2; j++) {
                    double uR = real[i + j];
                    double uI = imag[i + j];
                    double vR = real[i + j + len / 2] * curR - imag[i + j + len / 2] * curI;
                    double vI = real[i + j + len / 2] * curI + imag[i + j + len / 2] * curR;
                    real[i + j] = uR + vR;
                    imag[i + j] = uI + vI;
                    real[i + j + len / 2] = uR - vR;
                    imag[i + j + len / 2] = uI - vI;
                    double newCurR = curR * wR - curI * wI;
                    curI = curR * wI + curI * wR;
                    curR = newCurR;
                }
            }
        }
        if (inverse) {
            for (int i = 0; i < n; i++) {
                real[i] /= n;
                imag[i] /= n;
            }
        }
    }

}

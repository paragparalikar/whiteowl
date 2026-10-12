package com.whiteowl.core.backtest.discovery;

import lombok.extern.slf4j.Slf4j;
import smile.base.cart.DecisionNode;
import smile.base.cart.InternalNode;
import smile.base.cart.Node;
import smile.base.cart.OrdinalNode;
import smile.base.cart.SplitRule;
import smile.classification.DecisionTree;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.vector.BaseVector;
import smile.data.vector.DoubleVector;
import smile.data.vector.IntVector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Meta-labeling stage: for one signal's trade set, train a shallow decision
 * tree to predict trade profitability from the regime-feature snapshot, then
 * evaluate the filtered strategy ("take the trade only when the tree predicts
 * a win") on purged, embargoed walk-forward folds. Reports pooled out-of-sample
 * Sortino, permutation feature importance and the top leaf rules.
 */
@Slf4j
public final class MetaLabeler {

    private static final int FOLDS = 5;
    private static final int EMBARGO_BARS = 200;   // ~1 trading day of 5-min bars
    private static final int MAX_DEPTH = 4;
    private static final int MAX_NODES = 31;
    private static final int NODE_SIZE = 50;
    private static final int IMPORTANCE_ROWS = 2000;

    public record MetaReport(
            String signalName,
            int totalTrades,
            int usableTrades,
            double baseSortino,
            double oosSortino,
            double oosWinRate,
            double oosAvgNet,
            double oosTotalNet,
            int oosSelected,
            double oosAccuracy,
            List<String> topFeatures,
            List<String> topRules) {}

    public MetaReport run(String signalName, List<DiscoveryTrade> trades,
                          FeatureMatrix fm, Path dotDir) throws IOException {
        int p = fm.columnCount();
        int n = trades.size();
        if (n < 400) {
            log.info("{}: only {} trades, skipping meta-labeling", signalName, n);
            return new MetaReport(signalName, n, n, Double.NaN, Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, 0, Double.NaN, List.of(), List.of());
        }

        // ── Design matrix (column-major) + nominal label ────────────────
        double[][] x = new double[p][n];
        int[] y = new int[n];
        double[] net = new double[n];
        int[] sigBar = new int[n];
        for (int r = 0; r < n; r++) {
            DiscoveryTrade t = trades.get(r);
            sigBar[r] = t.signalBarIndex();
            net[r] = t.netPoints();
            y[r] = t.win() ? 1 : 0;
            for (int j = 0; j < p; j++) x[j][r] = fm.value(j, sigBar[r]);
        }

        // Median imputation: early-session bars legitimately lack some
        // features (e.g. opening-range stats inside the first 30 min); a
        // column median keeps those trades instead of biasing the sample.
        for (int j = 0; j < p; j++) {
            double[] col = x[j];
            double[] defined = java.util.Arrays.stream(col)
                    .filter(v -> !Double.isNaN(v)).sorted().toArray();
            double med = defined.length > 0 ? defined[defined.length / 2] : 0;
            for (int r = 0; r < n; r++) {
                if (Double.isNaN(col[r])) col[r] = med;
            }
        }
        DataFrame df = frameFrom(x, y, fm.names(), 0, n, p);
        Formula formula = Formula.lhs("label");
        List<Tuple> rows = df.stream().toList();

        // ── Purged walk-forward: fold k tests on rows [k*n/F, (k+1)*n/F), ──
        //    trains on everything strictly before (embargo by bar distance). ──
        double baseSortino = sortino(net);

        List<Double> selNet = new ArrayList<>();
        int correct = 0, evaluated = 0;

        for (int k = 1; k < FOLDS; k++) {
            int testStart = k * n / FOLDS;
            int testEnd = (k == FOLDS - 1) ? n : (k + 1) * n / FOLDS;
            int trainEnd = purgeEnd(sigBar, testStart);
            if (trainEnd < 200) continue;

            DecisionTree tree = DecisionTree.fit(formula,
                    frameFrom(x, y, fm.names(), 0, trainEnd, p),
                    SplitRule.GINI, MAX_DEPTH, MAX_NODES, NODE_SIZE);
            for (int r = testStart; r < testEnd; r++) {
                int pred = tree.predict(rows.get(r));
                evaluated++;
                if (pred == y[r]) correct++;
                if (pred == 1) selNet.add(net[r]);
            }
        }

        double oosSortino = sortino(toArray(selNet));
        double oosTotal = selNet.stream().mapToDouble(Double::doubleValue).sum();
        long oosWins = selNet.stream().filter(v -> v > 0).count();
        double accuracy = evaluated > 0 ? (double) correct / evaluated : Double.NaN;

        // ── Final tree on all usable data: rules + dot dump ─────────────
        DecisionTree full = DecisionTree.fit(formula, df, SplitRule.GINI,
                MAX_DEPTH, MAX_NODES, NODE_SIZE);
        if (dotDir != null) {
            Files.createDirectories(dotDir);
            Files.writeString(dotDir.resolve(signalName + ".dot"), full.dot());
        }
        List<String> rules = extractRules(full, fm.names(), n);

        // ── Permutation importance on pooled OOS predictions ────────────
        List<String> topFeatures = permutationImportance(formula, x, y, sigBar, n, p, fm.names());

        double oosWinRate = selNet.isEmpty() ? Double.NaN : (double) oosWins / selNet.size();
        double oosAvgNet = selNet.isEmpty() ? Double.NaN : oosTotal / selNet.size();
        return new MetaReport(signalName, trades.size(), n, baseSortino, oosSortino,
                oosWinRate, oosAvgNet, oosTotal, selNet.size(), accuracy, topFeatures, rules);
    }

    /** Train rows whose signal bar precedes the test fold minus the embargo. */
    private static int purgeEnd(int[] sigBar, int testStart) {
        int purgeBar = sigBar[testStart] - EMBARGO_BARS;
        int end = testStart;
        while (end > 0 && sigBar[end - 1] > purgeBar) end--;
        return end;
    }

    /**
     * Permutation importance: fold-wise pooled OOS accuracy loss when each
     * feature column is shuffled. Uses a fresh tree per fold on the same
     * purged splits and a capped subsample of test rows for speed.
     */
    private static List<String> permutationImportance(Formula formula, double[][] x,
                                                       int[] y, int[] sigBar, int n, int p,
                                                       List<String> names) {
        double[] drop = new double[p];
        Random rng = new Random(7);
        for (int k = 1; k < FOLDS; k++) {
            int testStart = k * n / FOLDS;
            int testEnd = (k == FOLDS - 1) ? n : (k + 1) * n / FOLDS;
            int trainEnd = purgeEnd(sigBar, testStart);
            if (trainEnd < 200) continue;

            DataFrame trainDf = frameFrom(x, y, names, 0, trainEnd, p);
            DecisionTree tree = DecisionTree.fit(formula, trainDf,
                    SplitRule.GINI, MAX_DEPTH, MAX_NODES, NODE_SIZE);

            List<Integer> testIdx = new ArrayList<>();
            for (int r = testStart; r < testEnd; r++) testIdx.add(r);
            if (testIdx.size() > IMPORTANCE_ROWS) {
                java.util.Collections.shuffle(testIdx, rng);
                testIdx = testIdx.subList(0, IMPORTANCE_ROWS);
            }
            DataFrame testDf = frameFrom(x, y, names, testIdx, p);
            List<Tuple> testRows = testDf.stream().toList();
            int base = countCorrect(tree, testRows, y, testIdx);

            for (int j = 0; j < p; j++) {
                DataFrame perm = permutedFrame(x, y, names, testIdx, p, j, rng);
                int shuf = countCorrect(tree, perm.stream().toList(), y, testIdx);
                drop[j] += (double) (base - shuf) / testIdx.size();
            }
        }
        record Imp(String name, double d) {}
        List<Imp> imps = new ArrayList<>();
        for (int j = 0; j < p; j++) imps.add(new Imp(names.get(j), drop[j]));
        imps.sort(Comparator.comparingDouble(Imp::d).reversed());
        List<String> top = new ArrayList<>();
        for (int i = 0; i < Math.min(12, imps.size()); i++) {
            if (imps.get(i).d() <= 0) break;
            top.add(String.format("%s(Δ%.4f)", imps.get(i).name(), imps.get(i).d()));
        }
        return top;
    }

    private static int countCorrect(DecisionTree tree, List<Tuple> rows, int[] y, List<Integer> idx) {
        int c = 0;
        for (int i = 0; i < rows.size(); i++) {
            if (tree.predict(rows.get(i)) == y[idx.get(i)]) c++;
        }
        return c;
    }

    private static DataFrame frameFrom(double[][] x, int[] y, List<String> names,
                                        int from, int to, int p) {
        BaseVector[] vecs = new BaseVector[p + 1];
        int[] yy = new int[to - from];
        for (int j = 0; j < p; j++) {
            vecs[j] = DoubleVector.of(names.get(j),
                    java.util.Arrays.copyOfRange(x[j], from, to));
        }
        System.arraycopy(y, from, yy, 0, to - from);
        vecs[p] = labelVector(yy);
        return DataFrame.of(vecs);
    }

    private static DataFrame frameFrom(double[][] x, int[] y, List<String> names,
                                        List<Integer> idx, int p) {
        BaseVector[] vecs = new BaseVector[p + 1];
        int[] yy = new int[idx.size()];
        for (int j = 0; j < p; j++) {
            double[] c = new double[idx.size()];
            for (int i = 0; i < idx.size(); i++) c[i] = x[j][idx.get(i)];
            vecs[j] = DoubleVector.of(names.get(j), c);
        }
        for (int i = 0; i < idx.size(); i++) yy[i] = y[idx.get(i)];
        vecs[p] = labelVector(yy);
        return DataFrame.of(vecs);
    }

    private static DataFrame permutedFrame(double[][] x, int[] y, List<String> names,
                                            List<Integer> idx, int p, int feature,
                                            Random rng) {
        BaseVector[] vecs = new BaseVector[p + 1];
        int[] yy = new int[idx.size()];
        for (int j = 0; j < p; j++) {
            double[] c = new double[idx.size()];
            for (int i = 0; i < idx.size(); i++) c[i] = x[j][idx.get(i)];
            vecs[j] = DoubleVector.of(names.get(j), c);
        }
        List<Double> vals = new ArrayList<>();
        for (double v : ((DoubleVector) vecs[feature]).array()) vals.add(v);
        java.util.Collections.shuffle(vals, rng);
        double[] shuf = vals.stream().mapToDouble(Double::doubleValue).toArray();
        vecs[feature] = DoubleVector.of(names.get(feature), shuf);
        for (int i = 0; i < idx.size(); i++) yy[i] = y[idx.get(i)];
        vecs[p] = labelVector(yy);
        return DataFrame.of(vecs);
    }

    private static IntVector labelVector(int[] y) {
        return IntVector.of(new StructField("label", DataTypes.IntegerType,
                new NominalScale("0", "1")), y);
    }

    // ── Rule extraction ────────────────────────────────────────────────

    private static List<String> extractRules(DecisionTree tree, List<String> names, int n) {
        List<RuleAcc> rules = new ArrayList<>();
        walk(tree.root(), names, new ArrayList<>(), rules);
        rules.sort(Comparator.comparingDouble(RuleAcc::winProb).reversed());
        List<String> top = new ArrayList<>();
        for (int i = 0; i < Math.min(10, rules.size()); i++) {
            RuleAcc r = rules.get(i);
            if (r.size() < 30) break;
            top.add(String.format("P(win)=%.2f n=%d :: %s",
                    r.winProb(), r.size(), String.join(" AND ", r.path())));
        }
        return top;
    }

    private static void walk(Node node, List<String> names, List<String> path,
                              List<RuleAcc> acc) {
        if (node instanceof InternalNode in) {
            String featureName = names.get(in.feature());
            double thr = splitValue(in);
            if (in.trueChild() != null) {
                path.add(featureName + " <= " + fmt(thr));
                walk(in.trueChild(), names, path, acc);
                path.remove(path.size() - 1);
            }
            if (in.falseChild() != null) {
                path.add(featureName + " > " + fmt(thr));
                walk(in.falseChild(), names, path, acc);
                path.remove(path.size() - 1);
            }
        } else if (node instanceof DecisionNode leaf) {
            int[] counts = leaf.count();
            int total = 0;
            for (int cc : counts) total += cc;
            double p1 = total > 0 && counts.length > 1 ? (double) counts[1] / total : 0;
            acc.add(new RuleAcc(new ArrayList<>(path), total, p1, leaf.output()));
        }
    }

    private record RuleAcc(List<String> path, int size, double winProb, int klass) {}

    private static double splitValue(InternalNode node) {
        if (node instanceof OrdinalNode on) {
            try {
                var f = OrdinalNode.class.getDeclaredField("value");
                f.setAccessible(true);
                return f.getDouble(on);
            } catch (ReflectiveOperationException e) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    private static String fmt(double v) {
        return Double.isNaN(v) ? "?" : String.format("%.4g", v);
    }

    // ── Metrics ────────────────────────────────────────────────────────

    public static double sortino(double[] r) {
        if (r.length == 0) return Double.NaN;
        double mean = 0;
        for (double v : r) mean += v;
        mean /= r.length;
        double dv = 0;
        for (double v : r) {
            double d = Math.min(v, 0);
            dv += d * d;
        }
        dv = Math.sqrt(dv / r.length);
        return dv > 0 ? mean / dv : (mean > 0 ? Double.POSITIVE_INFINITY : 0);
    }

    private static double[] toArray(List<Double> l) {
        double[] a = new double[l.size()];
        for (int i = 0; i < l.size(); i++) a[i] = l.get(i);
        return a;
    }
}

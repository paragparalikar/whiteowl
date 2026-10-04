import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Reads an AlgoTest trade-wise CSV and finds the daily profit target and
 * stop loss that would have maximised total P/L.
 *
 * Each day contributes: -stopLoss if Lowest MTM <= -stopLoss (checked first,
 * conservative), else +target if Highest MTM >= target, else the actual P/L.
 *
 * Run with:  java DailyTargetStopLoss.java
 */
public class DailyTargetStopLoss {

    // <<< Set the CSV file path here >>>
    private static final String FILE_PATH =
            "C:\\Users\\parag\\Downloads\\6ac0dda180185bc1a21c2bb7_1791024557.csv";

    private static final int COL_INDEX = 0;
    private static final int COL_PL = 15;
    private static final int COL_HIGHEST_MTM = 18;
    private static final int COL_LOWEST_MTM = 19;

    private record Day(double pl, double high, double low) {
    }

    public static void main(String[] args) throws IOException {
        List<Day> days = load(Path.of(FILE_PATH));
        if (days.isEmpty()) {
            System.out.println("No trade rows found in " + FILE_PATH);
            return;
        }

        // Candidate thresholds: the optimum only changes when target crosses a
        // day's Highest MTM or stop crosses a day's -Lowest MTM, so testing
        // every distinct observed value (plus "no limit") is exact.
        TreeSet<Double> targets = new TreeSet<>();
        TreeSet<Double> stops = new TreeSet<>();
        double baseline = 0;
        for (Day d : days) {
            targets.add(d.high);
            if (d.low < 0) {
                stops.add(-d.low);
            }
            baseline += d.pl;
        }
        targets.add(Double.POSITIVE_INFINITY); // no target
        stops.add(Double.POSITIVE_INFINITY);   // no stop loss

        double bestTotal = Double.NEGATIVE_INFINITY;
        double bestTarget = 0, bestStop = 0;
        for (double target : targets) {
            for (double stop : stops) {
                double total = simulate(days, target, stop);
                if (total > bestTotal) {
                    bestTotal = total;
                    bestTarget = target;
                    bestStop = stop;
                }
            }
        }

        System.out.println("File:              " + FILE_PATH);
        System.out.println("Trading days:      " + days.size());
        System.out.printf ("Baseline P/L:      %.2f%n", baseline);
        System.out.println("Best target:       " + fmt(bestTarget));
        System.out.println("Best stop loss:    " + fmt(bestStop));
        System.out.printf ("Best total P/L:    %.2f%n", bestTotal);
    }

    private static double simulate(List<Day> days, double target, double stop) {
        double total = 0;
        for (Day d : days) {
            if (d.low <= -stop) {
                total -= stop;
            } else if (d.high >= target) {
                total += target;
            } else {
                total += d.pl;
            }
        }
        return total;
    }

    private static List<Day> load(Path file) throws IOException {
        List<Day> days = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank() || line.startsWith("Index,")) {
                continue;
            }
            String[] c = line.split(",", -1);
            // Day summary rows have a whole-number index; leg rows are "1.1", "1.2", ...
            if (c.length <= COL_LOWEST_MTM || c[COL_INDEX].contains(".")) {
                continue;
            }
            days.add(new Day(
                    Double.parseDouble(c[COL_PL]),
                    Double.parseDouble(c[COL_HIGHEST_MTM]),
                    Double.parseDouble(c[COL_LOWEST_MTM])));
        }
        return days;
    }

    private static String fmt(double v) {
        return Double.isInfinite(v) ? "none" : String.format("%.2f", v);
    }
}

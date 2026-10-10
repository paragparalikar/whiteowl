package com.whiteowl.tools.continuousfutures;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;

/**
 * Builds a continuous front-contract 1-minute CSV per commodity from
 * per-day/per-expiry parquet files named:
 *   {SYMBOL}_FUT_1min_{yyyy-MM-dd}_exp{yyyy-MM-dd}.parquet
 *
 * Usage:
 *   ContinuousFutures --schema <file.parquet>         dump schema + first rows
 *   ContinuousFutures <dataset-root> <output-dir>     build continuous CSVs
 */
public class ContinuousFutures {

    private static final Pattern FILE_NAME =
            Pattern.compile("(.+)_FUT_1min_(\\d{4}-\\d{2}-\\d{2})_exp(\\d{4}-\\d{2}-\\d{2})\\.parquet");

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    record ContractFile(LocalDate tradeDate, LocalDate expiry, Path file) {}

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "--schema".equals(args[0])) {
            dumpSchema(Path.of(args[1]));
            return;
        }
        if (args.length == 2 && "--cat".equals(args[0])) {
            catFile(Path.of(args[1]));
            return;
        }
        if (args.length != 2) {
            System.err.println("Usage: ContinuousFutures --schema <file.parquet>");
            System.err.println("   or: ContinuousFutures <dataset-root> <output-dir>");
            System.exit(1);
        }
        Path root = Path.of(args[0]);
        Path outDir = Path.of(args[1]);
        Files.createDirectories(outDir);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path commodityDir : stream) {
                if (!Files.isDirectory(commodityDir)) {
                    continue;
                }
                buildContinuous(commodityDir, outDir);
            }
        }
    }

    private static void buildContinuous(Path commodityDir, Path outDir) throws IOException {
        String symbol = commodityDir.getFileName().toString();
        Path futDir = commodityDir.resolve("FUT_1min");
        if (!Files.isDirectory(futDir)) {
            System.out.println(symbol + ": no FUT_1min directory, skipping");
            return;
        }

        // Index all contract files by trade date
        Map<LocalDate, List<ContractFile>> byDate = new TreeMap<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(futDir, "*.parquet")) {
            for (Path f : files) {
                Matcher m = FILE_NAME.matcher(f.getFileName().toString());
                if (!m.matches()) {
                    continue;
                }
                LocalDate tradeDate = LocalDate.parse(m.group(2));
                LocalDate expiry = LocalDate.parse(m.group(3));
                byDate.computeIfAbsent(tradeDate, d -> new ArrayList<>())
                        .add(new ContractFile(tradeDate, expiry, f));
            }
        }
        if (byDate.isEmpty()) {
            System.out.println(symbol + ": no parquet files found, skipping");
            return;
        }

        Path outFile = outDir.resolve(symbol + "_continuous_1min.csv");
        long rows = 0;
        long dupsDropped = 0;
        int sparseDays = 0;
        LocalDate prevExpiry = null;
        try (BufferedWriter w = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
            w.write(String.join(",", OUT_COLUMNS));
            w.newLine();
            for (Map.Entry<LocalDate, List<ContractFile>> day : byDate.entrySet()) {
                LocalDate tradeDate = day.getKey();
                // Front contract = nearest expiry that is still tradeable on this day
                ContractFile front = day.getValue().stream()
                        .filter(c -> !c.expiry().isBefore(tradeDate))
                        .min(Comparator.comparing(ContractFile::expiry))
                        // fallback: if every expiry already passed, take the nearest one
                        .orElseGet(() -> day.getValue().stream()
                                .min(Comparator.comparing(ContractFile::expiry))
                                .orElseThrow());

                if (prevExpiry != null && !front.expiry().equals(prevExpiry)) {
                    System.out.printf("%s: roll on %s -> expiry %s%n", symbol, tradeDate, front.expiry());
                }
                prevExpiry = front.expiry();

                List<Row> dayRows = readRows(front.file());
                // Deduplicate timestamps (vendor files sometimes interleave a sparse
                // second feed with much lower OI) - keep the higher-OI row.
                Map<String, Row> byTs = new java.util.LinkedHashMap<>();
                for (Row r : dayRows) {
                    Row prev = byTs.get(r.dt());
                    if (prev == null || r.oi() > prev.oi()) {
                        byTs.put(r.dt(), r);
                    }
                }
                dupsDropped += dayRows.size() - byTs.size();
                if (byTs.size() < 30) {
                    // Phantom bars on closed days (e.g. exchange holidays) - skip entirely
                    System.out.printf("%s: skipped %s (only %d rows, likely closed day)%n",
                            symbol, tradeDate, byTs.size());
                    continue;
                }
                if (byTs.size() < 500) {
                    sparseDays++;
                    System.out.printf("%s: sparse day %s (%d rows, expiry %s)%n",
                            symbol, tradeDate, byTs.size(), front.expiry());
                }
                for (Row r : byTs.values()) {
                    w.write(String.join(",", r.cells()));
                    w.newLine();
                    rows++;
                }
            }
        }
        System.out.printf("%s: wrote %,d rows (dropped %,d dup rows, %d sparse days) -> %s%n",
                symbol, rows, dupsDropped, sparseDays, outFile);
    }

    private static final String[] OUT_COLUMNS = {
            "datetime", "open", "high", "low", "close", "volume", "open_interest", "expiry_date", "symbol"};

    private static final Map<String, List<String>> FIELD_ALIASES = Map.of(
            "datetime", List.of("datetime", "timestamp"),
            "open", List.of("open"),
            "high", List.of("high"),
            "low", List.of("low"),
            "close", List.of("close"),
            "volume", List.of("volume"),
            "open_interest", List.of("open_interest", "oi"),
            "expiry_date", List.of("expiry_date", "expiry"),
            "symbol", List.of("stock_code", "symbol", "tradingsymbol"));

    record Row(String dt, String[] cells, long oi) {}

    private static List<Row> readRows(Path file) throws IOException {
        List<Row> rows = new ArrayList<>();
        org.apache.hadoop.fs.Path hPath = new org.apache.hadoop.fs.Path(file.toUri());
        try (ParquetReader<Group> reader =
                ParquetReader.builder(new GroupReadSupport(), hPath).build()) {
            int[] colIdx = null;
            int oiIdx = -1;
            Group g;
            while ((g = reader.read()) != null) {
                if (colIdx == null) {
                    org.apache.parquet.schema.GroupType schema = g.getType();
                    colIdx = new int[OUT_COLUMNS.length];
                    for (int c = 0; c < OUT_COLUMNS.length; c++) {
                        colIdx[c] = -1;
                        for (String alias : FIELD_ALIASES.get(OUT_COLUMNS[c])) {
                            if (schema.containsField(alias)) {
                                colIdx[c] = schema.getFieldIndex(alias);
                                break;
                            }
                        }
                    }
                    oiIdx = colIdx[6]; // open_interest column position in OUT_COLUMNS
                }
                String[] cells = new String[OUT_COLUMNS.length];
                for (int c = 0; c < OUT_COLUMNS.length; c++) {
                    int idx = colIdx[c];
                    cells[c] = (idx >= 0 && g.getFieldRepetitionCount(idx) > 0)
                            ? formatField(g, idx)
                            : "";
                }
                long oi = Long.MIN_VALUE;
                if (oiIdx >= 0 && g.getFieldRepetitionCount(oiIdx) > 0) {
                    PrimitiveType p = g.getType().getType(oiIdx).asPrimitiveType();
                    if (p.getPrimitiveTypeName() == PrimitiveType.PrimitiveTypeName.INT64
                            || p.getPrimitiveTypeName() == PrimitiveType.PrimitiveTypeName.INT32) {
                        oi = g.getLong(oiIdx, 0);
                    }
                }
                rows.add(new Row(cells[0], cells, oi));
            }
        }
        return rows;
    }

    private static final List<String> PREFERRED_ORDER = List.of(
            "datetime", "open", "high", "low", "close", "volume", "open_interest", "expiry_date");

    // Field indices reordered so preferred columns come first, the rest in schema order
    private static int[] columnOrder(org.apache.parquet.schema.GroupType schema) {
        List<Type> fields = schema.getFields();
        int[] order = new int[fields.size()];
        int pos = 0;
        for (String name : PREFERRED_ORDER) {
            for (int i = 0; i < fields.size(); i++) {
                if (fields.get(i).getName().equals(name)) {
                    order[pos++] = i;
                }
            }
        }
        for (int i = 0; i < fields.size(); i++) {
            if (!PREFERRED_ORDER.contains(fields.get(i).getName())) {
                order[pos++] = i;
            }
        }
        return order;
    }

    private static String headerLine(Group g, int[] order) {
        StringBuilder sb = new StringBuilder();
        List<Type> fields = g.getType().getFields();
        for (int i = 0; i < order.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(fields.get(order[i]).getName());
        }
        return sb.toString();
    }

    private static String dataLine(Group g, int[] order) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < order.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            int field = order[i];
            if (g.getFieldRepetitionCount(field) == 0) {
                continue;
            }
            sb.append(formatField(g, field));
        }
        return sb.toString();
    }

    private static String dataLine(Group g) {
        return dataLine(g, columnOrder(g.getType()));
    }

    private static String formatField(Group g, int index) {
        Type type = g.getType().getType(index);
        if (!type.isPrimitive()) {
            return g.getGroup(index, 0).toString();
        }
        PrimitiveType p = type.asPrimitiveType();
        LogicalTypeAnnotation logical = p.getLogicalTypeAnnotation();
        switch (p.getPrimitiveTypeName()) {
            case INT32 -> {
                int v = g.getInteger(index, 0);
                if (logical instanceof LogicalTypeAnnotation.DateLogicalTypeAnnotation) {
                    return LocalDate.ofEpochDay(v).toString();
                }
                return Integer.toString(v);
            }
            case INT64 -> {
                long v = g.getLong(index, 0);
                if (logical instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation ts) {
                    Instant instant = switch (ts.getUnit()) {
                        case MILLIS -> Instant.ofEpochMilli(v);
                        case MICROS -> Instant.ofEpochSecond(v / 1_000_000, (v % 1_000_000) * 1_000);
                        case NANOS -> Instant.ofEpochSecond(v / 1_000_000_000, v % 1_000_000_000);
                    };
                    return ts.isAdjustedToUTC()
                            ? TS_FMT.format(instant.atZone(IST))
                            : TS_FMT.format(LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC));
                }
                if (logical instanceof LogicalTypeAnnotation.TimeLogicalTypeAnnotation) {
                    return Long.toString(v);
                }
                return Long.toString(v);
            }
            case FLOAT -> {
                return Float.toString(g.getFloat(index, 0));
            }
            case DOUBLE -> {
                return Double.toString(g.getDouble(index, 0));
            }
            case BOOLEAN -> {
                return Boolean.toString(g.getBoolean(index, 0));
            }
            case INT96 -> {
                java.nio.ByteBuffer buf = g.getInt96(index, 0).toByteBuffer()
                        .order(java.nio.ByteOrder.LITTLE_ENDIAN);
                long nanosOfDay = buf.getLong();
                int julianDay = buf.getInt();
                long epochDay = julianDay - 2_440_588L;
                return TS_FMT.format(LocalDateTime.ofEpochSecond(
                        epochDay * 86_400 + nanosOfDay / 1_000_000_000,
                        (int) (nanosOfDay % 1_000_000_000),
                        java.time.ZoneOffset.UTC));
            }
            default -> {
                Binary b = g.getBinary(index, 0);
                if (logical instanceof LogicalTypeAnnotation.StringLogicalTypeAnnotation) {
                    return escapeCsv(b.toStringUsingUTF8());
                }
                return b.toStringUsingUTF8();
            }
        }
    }

    private static String escapeCsv(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    private static void catFile(Path file) throws IOException {
        for (Row r : readRows(file)) {
            System.out.println(String.join(",", r.cells()));
        }
    }

    private static void dumpSchema(Path file) throws IOException {
        Configuration conf = new Configuration();
        org.apache.hadoop.fs.Path hPath = new org.apache.hadoop.fs.Path(file.toUri());
        ParquetMetadata meta = ParquetFileReader.readFooter(conf, hPath);
        System.out.println("=== Schema ===");
        System.out.println(meta.getFileMetaData().getSchema());
        System.out.println("=== Key-value metadata ===");
        meta.getFileMetaData().getKeyValueMetaData().forEach((k, v) -> System.out.println(k + " = " + v));
        System.out.println("=== Column encodings ===");
        for (ColumnDescriptor c : meta.getFileMetaData().getSchema().getColumns()) {
            System.out.println(java.util.Arrays.toString(c.getPath()));
        }
        System.out.println("=== First 5 rows ===");
        try (ParquetReader<Group> reader =
                ParquetReader.builder(new GroupReadSupport(), hPath).build()) {
            for (int i = 0; i < 5; i++) {
                Group g = reader.read();
                if (g == null) {
                    break;
                }
                System.out.println(g);
                System.out.println("  formatted: " + dataLine(g));
            }
        }
        // keep hadoop fs happy on exit
        HadoopInputFile.fromPath(hPath, conf);
    }
}

package com.whiteowl.core.backtest.driver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates {@code C:\trading\etf_invvol_allocator.xlsx} — upload to Google
 * Drive and open with Google Sheets. It pulls weekly closes for the 8-ETF
 * universe via GOOGLEFINANCE and computes the Inverse-Volatility (4-week
 * lookback) allocation of the weekly Rs 40,000.
 *
 * <p>Sheet layout: hidden {@code Data} tab holds one GOOGLEFINANCE spill per
 * ETF (Date | Close, weekly, last ~100 days); {@code Main} tab picks the last
 * 5 closes per ETF, computes the 4 weekly returns, their sample stdev,
 * 1/vol, normalized weight and the Rs allocation.</p>
 */
public final class EtfInvVolSheetGenerator {

    private static final Path OUTPUT = Path.of("C:\\trading\\etf_invvol_allocator.xlsx");

    /** File ticker, NSE symbol for GOOGLEFINANCE, Data-tab close column. */
    private record Etf(String ticker, String nseSymbol, char closeCol, char dateCol) {}

    // Data tab: each GOOGLEFINANCE spills Date|Close into two columns, ETFs
    // spaced 3 columns apart starting at A.
    private static final List<Etf> ETFS = List.of(
            new Etf("HFCM", "HDFCSML250", 'B', 'A'),
            new Etf("HNGS", "HNGSNGBEES", 'E', 'D'),
            new Etf("KOTS", "SILVER1",    'H', 'G'),
            new Etf("MIAN", "MIDCAPETF",  'K', 'J'),
            new Etf("MIRP", "MASPTOP50",  'N', 'M'),
            new Etf("MIRS", "MAFANG",     'Q', 'P'),
            new Etf("NBES", "NIFTYBEES",  'T', 'S'),
            new Etf("TATG", "TATAGOLD",   'W', 'V'));

    private static final int FIRST_ROW = 5;
    private static final int LAST_ROW  = FIRST_ROW + ETFS.size() - 1;

    public static void main(String[] args) throws IOException {
        try (OutputStream fos = Files.newOutputStream(OUTPUT);
             ZipOutputStream zip = new ZipOutputStream(fos)) {
            put(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                      <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>""");
            put(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>""");
            put(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets>
                        <sheet name="Main" sheetId="1" r:id="rId1"/>
                        <sheet name="Data" sheetId="2" r:id="rId2"/>
                      </sheets>
                    </workbook>""");
            put(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                      <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                    </Relationships>""");
            put(zip, "xl/worksheets/sheet1.xml", mainSheet());
            put(zip, "xl/worksheets/sheet2.xml", dataSheet());
        }
        System.out.println("Wrote " + OUTPUT);
    }

    // ── Sheet builders ────────────────────────────────────────────────

    private static String mainSheet() {
        StringBuilder sb = new StringBuilder();
        sb.append(sheetOpen());
        row(sb, 1, text("A1", "Weekly allocation (Rs)"), num("B1", "40000"),
                text("D1", "<- change capital / lookback here"));
        row(sb, 2, text("A2", "Vol lookback (weeks)"), num("B2", "4"));
        row(sb, 4, new String[]{
                text("A4", "Ticker"), text("B4", "NSE Symbol"),
                text("C4", "Close T"), text("D4", "Close T-1"), text("E4", "Close T-2"),
                text("F4", "Close T-3"), text("G4", "Close T-4"),
                text("H4", "Ret W-1"), text("I4", "Ret W-2"), text("J4", "Ret W-3"),
                text("K4", "Ret W-4"), text("L4", "Vol (4w)"), text("M4", "1/Vol"),
                text("N4", "Weight"), text("O4", "Allocation (Rs)"),
                text("P4", "Qty")});

        int r = FIRST_ROW;
        for (Etf e : ETFS) {
            String col = String.valueOf(e.closeCol());
            row(sb, r,
                    text("A" + r, e.ticker()),
                    text("B" + r, e.nseSymbol()),
                    formula("C" + r, idx(col, "+1")),
                    formula("D" + r, idx(col, "")),
                    formula("E" + r, idx(col, "-1")),
                    formula("F" + r, idx(col, "-2")),
                    formula("G" + r, idx(col, "-3")),
                    formula("H" + r, "C" + r + "/D" + r + "-1"),
                    formula("I" + r, "D" + r + "/E" + r + "-1"),
                    formula("J" + r, "E" + r + "/F" + r + "-1"),
                    formula("K" + r, "F" + r + "/G" + r + "-1"),
                    formula("L" + r, "IFERROR(STDEV(H" + r + ":K" + r + "),\"\")"),
                    formula("M" + r, "IF(L" + r + "=\"\",\"\",1/L" + r + ")"),
                    formula("N" + r, "IF(M" + r + "=\"\",\"\",M" + r
                            + "/SUM($M$" + FIRST_ROW + ":$M$" + LAST_ROW + "))"),
                    formula("O" + r, "IF(N" + r + "=\"\",\"\",N" + r + "*$B$1)"),
                    formula("P" + r, "IF(O" + r + "=\"\",\"\",IFERROR(INT(O" + r + "/C" + r + "),\"\"))"));
            r++;
        }
        row(sb, LAST_ROW + 2,
                text("A" + (LAST_ROW + 2), "Total"),
                formula("O" + (LAST_ROW + 2), "SUM(O" + FIRST_ROW + ":O" + LAST_ROW + ")"));
        row(sb, LAST_ROW + 4,
                text("A" + (LAST_ROW + 4), "Weights = (1/vol) normalized over ETFs with a computable "
                        + "4-week stdev. Qty = whole units to buy (allocation / latest close, "
                        + "rounded down). If a GOOGLEFINANCE cell errors on the Data tab, fix the "
                        + "NSE symbol in column B here — or paste investing.com weekly closes "
                        + "(oldest at top) directly into that ETF's Date/Close columns on the "
                        + "Data tab."));
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    /** INDEX(Data!X:X, COUNT(Data!X:X)<offset>) — the Nth-from-last weekly close. */
    private static String idx(String col, String offset) {
        return "INDEX(Data!" + col + ":" + col + ",COUNT(Data!" + col + ":" + col + ")" + offset + ")";
    }

    private static String dataSheet() {
        StringBuilder sb = new StringBuilder();
        sb.append(sheetOpen());
        String[] cells = ETFS.stream()
                .map(e -> formula(e.dateCol() + "1", "GOOGLEFINANCE(\"NSE:" + e.nseSymbol()
                        + "\",\"price\",TODAY()-100,TODAY(),\"WEEKLY\")"))
                .toArray(String[]::new);
        row(sb, 1, cells);
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    // ── Minimal sheet-XML helpers ─────────────────────────────────────

    private static String sheetOpen() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<sheetData>";
    }

    private static void row(StringBuilder sb, int r, String... cells) {
        sb.append("<row r=\"").append(r).append("\">");
        for (String c : cells) {
            sb.append(c);
        }
        sb.append("</row>");
    }

    private static String text(String ref, String value) {
        return "<c r=\"" + ref + "\" t=\"inlineStr\"><is><t>" + esc(value) + "</t></is></c>";
    }

    private static String num(String ref, String value) {
        return "<c r=\"" + ref + "\"><v>" + value + "</v></c>";
    }

    private static String formula(String ref, String f) {
        return "<c r=\"" + ref + "\"><f>" + esc(f) + "</f></c>";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}

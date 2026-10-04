package com.whiteowl.core.backtest.algotest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whiteowl.core.backtest.algotest.model.AlgoStrategy;
import com.whiteowl.core.backtest.algotest.model.BacktestRequest;
import com.whiteowl.core.backtest.algotest.model.BacktestResult;
import com.whiteowl.core.backtest.algotest.model.LegConfig;
import com.whiteowl.core.backtest.algotest.model.enums.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

public class SerializationCheck {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = AlgoTestClient.mapper();

        AlgoStrategy strategy = new AlgoStrategy();
        strategy.setTicker(Ticker.SENSEX);
        strategy.setEntryTime(9, 20);
        strategy.setExitTime(15, 25);
        strategy.setMaxPositionInADay(1);

        LegConfig ce = new LegConfig();
        ce.setId("3i0g4xdi");
        ce.setPositionType(PositionType.SELL);
        ce.setInstrumentKind(LegType.CE);
        ce.setLots(LotType.QUANTITY, 10);
        ce.setStrike(StrikeType.OTM4);
        ce.setStopLoss(LegTgtSLType.POINTS, 25);
        strategy.addLeg(ce);

        LegConfig pe = new LegConfig();
        pe.setId("45wgeabw");
        pe.setPositionType(PositionType.SELL);
        pe.setInstrumentKind(LegType.PE);
        pe.setLots(LotType.QUANTITY, 10);
        pe.setStrike(StrikeType.OTM4);
        pe.setStopLoss(LegTgtSLType.POINTS, 25);
        strategy.addLeg(pe);

        BacktestRequest req = BacktestRequest.of(strategy, "2023-03-01", "2026-10-03");
        req.getAttributes().setTemplate("Straddle920");

        // parse the sample curl command's --data-raw payload (cmd.exe caret escaping)
        String requestFile = Files.readString(Path.of("src/main/resources/request-sample.txt"));
        String raw = requestFile.substring(requestFile.indexOf("--data-raw") + "--data-raw".length()).trim();
        String unescaped = raw.replaceAll("\\^(?s)(.)", "$1").trim();      // ^X -> literal X
        JsonNode expected = mapper.readTree(mapper.readTree(unescaped).asText()); // outer quoted string
        JsonNode actual = mapper.readTree(mapper.writeValueAsString(req));
        if (expected.equals(actual)) {
            System.out.println("REQUEST MATCH: generated request identical to sample");
        } else {
            System.out.println("REQUEST DIFF found:");
            diff("", expected, actual);
        }

        // deep-copy round trip (optimizer clones the template this way)
        BacktestRequest copy = mapper.readValue(mapper.writeValueAsBytes(req), BacktestRequest.class);
        JsonNode copyTree = mapper.readTree(mapper.writeValueAsString(copy));
        if (expected.equals(copyTree)) {
            System.out.println("COPY MATCH: clone -> serialize identical");
        } else {
            System.out.println("COPY DIFF found:");
            diff("", expected, copyTree);
        }

        // response deserialization
        String responseFile = Files.readString(Path.of("src/main/resources/response-sample.txt"));
        String body = responseFile.substring(responseFile.indexOf("Response:") + 9).trim();
        BacktestResult result = mapper.readValue(body, BacktestResult.class);
        System.out.println("RESPONSE: id=" + result.getId() + " status=" + result.getStatus()
                + " trades=" + result.getResults().getSummary().getNumberOfTrades()
                + " profit=" + result.getResults().getSummary().getOverallProfit()
                + " path=" + result.getResults().getResultFilePath());

        // flatten + csv round trip
        LinkedHashMap<String, String> row = ResultFlattener.flatten(req, result, null);
        Path csv = Path.of("target/sample-row.csv");
        OptimizationCsvWriter.write(csv, List.of(row));
        System.out.println("CSV columns: " + row.size());
        System.out.println(Files.readString(csv).lines().findFirst().orElse(""));
        System.out.println(Files.readString(csv).lines().skip(1).findFirst().orElse(""));
    }

    static void diff(String path, JsonNode exp, JsonNode act) {
        if (exp == null || act == null || !exp.getNodeType().equals(act.getNodeType())) {
            System.out.println(path + ": expected=" + exp + " actual=" + act);
            return;
        }
        if (exp.isObject()) {
            var names = new java.util.TreeSet<String>();
            exp.fieldNames().forEachRemaining(names::add);
            act.fieldNames().forEachRemaining(names::add);
            for (String n : names) diff(path + "." + n, exp.get(n), act.get(n));
        } else if (exp.isArray()) {
            if (exp.size() != act.size()) System.out.println(path + ": size " + exp.size() + " vs " + act.size());
            for (int i = 0; i < Math.min(exp.size(), act.size()); i++) diff(path + "[" + i + "]", exp.get(i), act.get(i));
        } else if (!exp.equals(act)) {
            System.out.println(path + ": expected=" + exp + " actual=" + act);
        }
    }
}

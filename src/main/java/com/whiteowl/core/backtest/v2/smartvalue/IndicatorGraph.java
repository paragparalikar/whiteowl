package com.whiteowl.core.backtest.v2.smartvalue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IndicatorGraph {

    private final Map<String, IndicatorNode> nodesBySignature = new LinkedHashMap<>();
    private final List<IndicatorNode> topologicalOrder = new ArrayList<>();
    private boolean dirty;

    public void addNode(IndicatorNode node) {
        nodesBySignature.put(node.getSignature(), node);
        dirty = true;
    }

    public IndicatorNode findBySignature(String signature) {
        return nodesBySignature.get(signature);
    }

    public boolean hasSignature(String signature) {
        return nodesBySignature.containsKey(signature);
    }

    public void updateAll() {
        if (dirty) {
            rebuildOrder();
        }
        for (IndicatorNode node : topologicalOrder) {
            node.update();
        }
    }

    private void rebuildOrder() {
        topologicalOrder.clear();
        Map<FloatSmartValue, IndicatorNode> outputToNode = new LinkedHashMap<>();
        for (IndicatorNode node : nodesBySignature.values()) {
            outputToNode.put(node.getOutput(), node);
        }
        Map<IndicatorNode, Boolean> visited = new LinkedHashMap<>();
        Map<IndicatorNode, Boolean> resolved = new LinkedHashMap<>();
        for (IndicatorNode node : nodesBySignature.values()) {
            if (!visited.containsKey(node)) {
                visit(node, outputToNode, visited, resolved);
            }
        }
        dirty = false;
    }

    private void visit(IndicatorNode node, Map<FloatSmartValue, IndicatorNode> outputToNode,
                        Map<IndicatorNode, Boolean> visited, Map<IndicatorNode, Boolean> resolved) {
        visited.put(node, Boolean.TRUE);
        FloatSmartValue output = node.getOutput();
        List<FloatSmartValue> dependencies = collectDependencies(output);
        for (FloatSmartValue dep : dependencies) {
            IndicatorNode depNode = outputToNode.get(dep);
            if (depNode != null && !visited.containsKey(depNode)) {
                visit(depNode, outputToNode, visited, resolved);
            }
        }
        if (!resolved.containsKey(node)) {
            topologicalOrder.add(node);
            resolved.put(node, Boolean.TRUE);
        }
    }

    private List<FloatSmartValue> collectDependencies(FloatSmartValue output) {
        if (output instanceof DerivedFloatSmartValue derived) {
            return List.of(derived.getLeft(), derived.getRight());
        }
        if (output instanceof ScalarDerivedFloatSmartValue scalar) {
            return List.of(scalar.getSource());
        }
        return Collections.emptyList();
    }

    public int size() {
        return nodesBySignature.size();
    }

}

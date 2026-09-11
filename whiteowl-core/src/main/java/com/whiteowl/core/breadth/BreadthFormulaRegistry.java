package com.whiteowl.core.breadth;

import com.whiteowl.core.breadth.formula.AdvanceDeclineBreadth;
import com.whiteowl.core.breadth.formula.MoneyFlowBreadth;
import com.whiteowl.core.breadth.formula.PercentageBreadth;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class BreadthFormulaRegistry {

    private final Map<String, BreadthFormula> formulas;
    private final List<BreadthFormula> formulaList;

    public BreadthFormulaRegistry() {
        this.formulaList = List.of(
                new AdvanceDeclineBreadth(),
                new PercentageBreadth(),
                new MoneyFlowBreadth()
        );
        this.formulas = formulaList.stream()
                .collect(Collectors.toMap(BreadthFormula::name, Function.identity()));
    }

    public BreadthFormula get(String name) {
        return formulas.get(name);
    }

    public List<BreadthFormula> getAll() {
        return formulaList;
    }

    public List<String> getNames() {
        return formulaList.stream().map(BreadthFormula::name).toList();
    }
}

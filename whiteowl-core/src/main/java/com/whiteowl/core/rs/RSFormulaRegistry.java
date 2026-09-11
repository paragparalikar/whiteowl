package com.whiteowl.core.rs;

import com.whiteowl.core.rs.formula.MansfieldRSFormula;
import com.whiteowl.core.rs.formula.RSLineFormula;
import com.whiteowl.core.rs.formula.RSRankFormula;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class RSFormulaRegistry {

    private final Map<String, RSFormula> formulas;
    private final List<RSFormula> formulaList;

    public RSFormulaRegistry() {
        this.formulaList = List.of(
                new RSLineFormula(),
                new MansfieldRSFormula(),
                new RSRankFormula()
        );
        this.formulas = formulaList.stream()
                .collect(Collectors.toMap(RSFormula::name, Function.identity()));
    }

    public RSFormula get(String name) {
        return formulas.get(name);
    }

    public List<String> getNames() {
        return formulaList.stream().map(RSFormula::name).toList();
    }
}

package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.whiteowl.core.backtest.algotest.model.enums.IndicatorTreeNodeType;
import com.whiteowl.core.backtest.algotest.model.enums.OperandType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Recursive indicator tree node. An operand node combines child nodes with
 * {@code OperandType} (e.g. And); a data node wraps an {@link IndicatorData}.
 * {@code Value} is therefore either {@code List<IndicatorNode>} or
 * {@link IndicatorData} depending on the node type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndicatorNode {

    /** "IndicatorTreeNodeType.OperandNode" or "IndicatorTreeNodeType.DataNode". */
    private String type;

    /** Only present on operand nodes, e.g. "OperandType.And". */
    private String operandType;

    /** Child nodes for operand nodes; {@link IndicatorData} for data nodes. */
    private Object value;

    public static IndicatorNode data(IndicatorData data) {
        return new IndicatorNode(IndicatorTreeNodeType.DATA_NODE.getApiValue(), null, data);
    }

    public static IndicatorNode operand(OperandType operandType, List<IndicatorNode> children) {
        return new IndicatorNode(IndicatorTreeNodeType.OPERAND_NODE.getApiValue(),
                operandType.getApiValue(), children);
    }

    /** Root operand node wrapping a single time indicator, e.g. "enter at 09:20". */
    public static IndicatorNode timeOperand(int hour, int minute) {
        return operand(OperandType.AND, List.of(data(IndicatorData.time(hour, minute))));
    }
}

package com.whiteowl.core.backtest.algotest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request attributes block. {@code template} is an optional strategy-template
 * label echoed back in results; {@code positional} is the "True"/"False" string.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BacktestAttributes {
    private String template;
    private String positional = "False";
}

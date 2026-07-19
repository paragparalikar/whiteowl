package com.whiteowl.core.review.model;

import com.whiteowl.workbench.charting.ChartOrderService;

public record AutofixContext(ChartOrderService chartOrderService, double stopLossPercentage) {

}

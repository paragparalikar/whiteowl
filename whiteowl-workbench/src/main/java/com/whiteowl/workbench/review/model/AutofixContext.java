package com.whiteowl.workbench.review.model;

import com.whiteowl.core.order.service.ChartOrderService;

public record AutofixContext(ChartOrderService chartOrderService, double stopLossPercentage) {

}

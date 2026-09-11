package com.whiteowl.workbench.alert;

import com.whiteowl.core.bar.model.Timeframe;

public record Alert(String name, String groupName, String screenName, Timeframe timeframe) {
}

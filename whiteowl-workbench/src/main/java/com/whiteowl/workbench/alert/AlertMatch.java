package com.whiteowl.workbench.alert;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.scrip.model.Scrip;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertMatch {

    private AlertDefinition alertDefinition;
    private Instant timestamp;
    private Scrip scrip;

    public AlertMatch(AlertDefinition alertDefinition, Scrip scrip) {
        this.alertDefinition = alertDefinition;
        this.scrip = scrip;
        this.timestamp = Instant.now();
    }

    public String getScreenId() {
        return alertDefinition != null ? alertDefinition.getScreenId() : null;
    }

    public Timeframe getTimeframe() {
        return alertDefinition != null ? alertDefinition.getTimeframe() : null;
    }

}

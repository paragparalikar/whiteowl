package com.whiteowl.workbench.alert;

import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.core.scrip.model.Scrip;
import com.whiteowl.scripting.screener.Screen;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDefinition {

    private String id;
    private String name;
    private String groupId;
    private String screenId;
    private Timeframe timeframe;
    @Builder.Default
    private boolean enabled = true;

    public AlertDefinition(String name, String groupId, String screenId, Timeframe timeframe) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.groupId = groupId;
        this.screenId = screenId;
        this.timeframe = timeframe;
        this.enabled = true;
    }
}

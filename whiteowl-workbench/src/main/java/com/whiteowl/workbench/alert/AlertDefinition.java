package com.whiteowl.workbench.alert;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.whiteowl.core.bar.model.Timeframe;
import com.whiteowl.workbench.collection.CollectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDefinition {

    private String id;
    private String name;
    private CollectionType sourceType;
    private String sourceId;
    private String screenId;
    private Timeframe timeframe;
    @Builder.Default
    private boolean enabled = true;

    public AlertDefinition(String name, CollectionType sourceType, String sourceId,
                           String screenId, Timeframe timeframe) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.screenId = screenId;
        this.timeframe = timeframe;
        this.enabled = true;
    }

    /** Back-compat: pre-v2 files stored the source under "groupId". */
    @JsonSetter("groupId")
    private void setLegacyGroupId(String legacyGroupId) {
        if (this.sourceId == null && legacyGroupId != null) {
            this.sourceId = legacyGroupId;
            if (this.sourceType == null) {
                this.sourceType = CollectionType.GROUP;
            }
        }
    }
}

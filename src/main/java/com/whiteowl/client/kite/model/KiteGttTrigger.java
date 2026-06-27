package com.whiteowl.client.kite.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@Jacksonized
public final class KiteGttTrigger {

    private int id;
    private String userId;
    private Integer parentTrigger;
    private KiteGttType type;
    private String createdAt;
    private String updatedAt;
    private String expiresAt;
    private KiteGttStatus status;
    private KiteGttCondition condition;
    private List<KiteGttOrder> orders;
    private Map<String, Object> meta;

}

package com.whiteowl.client.kite.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class KiteTwofa {

    private String userId;
    private String requestId;
    private String twofaType;
    private String[] twofaTypes;
    private String twofaStatus;
    private boolean captcha;
    private boolean locked;

}

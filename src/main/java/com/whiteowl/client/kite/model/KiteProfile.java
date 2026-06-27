package com.whiteowl.client.kite.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public final class KiteProfile {

    private String userId;
    private String userName;
    private String userType;
    private String email;
    private String phone;
    private String broker;
    private String pan;
    private String userShortname;
    private String avatarUrl;
    private List<String> dpIds;
    private List<KiteProduct> products;
    private List<KiteLimitType> orderTypes;
    private List<KiteExchange> exchanges;

}

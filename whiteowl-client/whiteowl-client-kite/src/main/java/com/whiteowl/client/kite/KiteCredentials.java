package com.whiteowl.client.kite;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class KiteCredentials {

    private final String portfolioId;
    private final String username;
    private final String password;
    private final String pin;

}

package com.whiteowl.client.kite.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class KiteSession {

    private String username;
    private String enctoken;
    private long createdAt;

}

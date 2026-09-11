package com.whiteowl.client.kite.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class KiteResponse<T> {

    private T data;
    private String status;
    private String message;
    @JsonProperty("error_type") private String errorType;

}

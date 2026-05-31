package com.fidd.ydisk.rest.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableYandexApiError.class)
public interface YandexApiError {
    @JsonProperty("message")
    String message();

    @JsonProperty("description")
    String description();

    @JsonProperty("error")
    String error();
}


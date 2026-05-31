package com.fidd.ydisk.rest.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableLink.class)
public interface Link {
    @JsonProperty("href")
    String href();

    @JsonProperty("method")
    String method();

    @JsonProperty("templated")
    boolean templated();
}


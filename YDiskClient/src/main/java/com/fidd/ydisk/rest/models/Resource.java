package com.fidd.ydisk.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonDeserialize(as = ImmutableResource.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface Resource {
    @JsonProperty("name")
    @Nullable
    String name();

    @JsonProperty("type")
    String type();

    @JsonProperty("path")
    String path();

    @JsonProperty("size")
    @Nullable
    Long size();

    @JsonProperty("_embedded")
    @Nullable
    ResourceList embedded();
}


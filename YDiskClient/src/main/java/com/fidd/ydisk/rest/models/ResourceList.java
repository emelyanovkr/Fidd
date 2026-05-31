package com.fidd.ydisk.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonDeserialize(as = ImmutableResourceList.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface ResourceList {
    @JsonProperty("items")
    List<Resource> items();

    @JsonProperty("limit")
    @Nullable
    Integer limit();

    @JsonProperty("offset")
    @Nullable
    Integer offset();
}

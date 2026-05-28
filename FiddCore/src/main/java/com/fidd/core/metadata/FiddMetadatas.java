package com.fidd.core.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddMetadatas.class)
@JsonDeserialize(as = ImmutableFiddMetadatas.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface FiddMetadatas {
    FiddFileMetadata fiddFileMetadata();
    List<LogicalFileMetadata> logicalFileMetadatas();
}

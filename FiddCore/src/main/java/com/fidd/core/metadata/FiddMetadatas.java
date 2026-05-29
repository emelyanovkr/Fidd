package com.fidd.core.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.service.LogicalFileInfo;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddMetadatas.class)
@JsonDeserialize(as = ImmutableFiddMetadatas.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface FiddMetadatas {
    FiddFileMetadata fiddFileMetadata();
    List<LogicalFileInfo> logicalFileInfos();

    static FiddMetadatas of(FiddFileMetadata fiddFileMetadata, List<LogicalFileInfo> logicalFileInfos) {
        return ImmutableFiddMetadatas.builder().fiddFileMetadata(fiddFileMetadata).logicalFileInfos(logicalFileInfos).build();
    }
}

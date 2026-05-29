package com.fidd.service;

import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.metadata.FiddMetadatas;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.List;

public interface FiddContentService {
    /** Descending order */
    List<Long> getMessageNumbersTail(int count);
    /** Descending order */
    List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive);
    /** Descending order */
    List<Long> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest,
                                        long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest);

    // We want to load all Metadata in one go and cache
    default @Nullable FiddFileMetadata getFiddFileMetadata(long messageNumber) {
        FiddMetadatas fiddMetadatas = getFiddMetadatas(messageNumber);
        return fiddMetadatas == null ? null : fiddMetadatas.fiddFileMetadata();
    }
    default @Nullable List<LogicalFileInfo> getLogicalFileInfos(long messageNumber) {
        FiddMetadatas fiddMetadatas = getFiddMetadatas(messageNumber);
        return fiddMetadatas == null ? null : fiddMetadatas.logicalFileInfos();
    }

    @Nullable FiddMetadatas getFiddMetadatas(long messageNumber);

    @Nullable InputStream readLogicalFile(long messageNumber, LogicalFileInfo logicalFileInfo);
    @Nullable InputStream readLogicalFileChunk(long messageNumber, LogicalFileInfo logicalFileInfo, long offset, long length);

    // ---------------------------------------------
    //    TODO: Validations; CRC; progressive CRC
    // ---------------------------------------------
}

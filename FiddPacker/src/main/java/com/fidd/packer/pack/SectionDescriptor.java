package com.fidd.packer.pack;

import com.fidd.core.fiddfile.FiddFileMetadata;

public abstract class SectionDescriptor {
    long gapBefore;

    public long getGapBefore() { return gapBefore; }
    public void setGapBefore(long gapBefore) { this.gapBefore = gapBefore; }
}

class FiddFileMetadataSectionDescriptor extends SectionDescriptor {
    public final FiddFileMetadata fiddFileMetadata;

    FiddFileMetadataSectionDescriptor(FiddFileMetadata fiddFileMetadata) {
        this.fiddFileMetadata = fiddFileMetadata;
    }
}

class LogicalFileSectionDescriptor extends SectionDescriptor {
    public final FilePathTuple filePathTuple;

    public LogicalFileSectionDescriptor(FilePathTuple filePathTuple) {
        this.filePathTuple = filePathTuple;
    }
}

class LogicalFileMetadataHeaderSectionDescriptor extends SectionDescriptor {
    public final FilePathTuple filePathTuple;

    public LogicalFileMetadataHeaderSectionDescriptor(FilePathTuple filePathTuple) {
        this.filePathTuple = filePathTuple;
    }
}
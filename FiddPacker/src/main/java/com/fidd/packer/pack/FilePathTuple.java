package com.fidd.packer.pack;

import javax.annotation.Nullable;
import java.io.File;

public class FilePathTuple {
    public final File file;
    public final String relativePath;
    @Nullable LengthAndCrcs logicalFileSectionLengthAndCrc = null;
    @Nullable LengthAndCrcs logicalFileMetadataSectionLengthAndCrc = null;
    @Nullable byte[] logicalFileSectionKey = null;

    public FilePathTuple(File file, String relativePath) {
        this.file = file;
        this.relativePath = relativePath;
    }

    public File file() {
        return file;
    }

    public String relativePath() {
        return relativePath;
    }

    public @Nullable LengthAndCrcs getLogicalFileSectionLengthAndCrc() {
        return logicalFileSectionLengthAndCrc;
    }

    public void setLogicalFileSectionLengthAndCrc(LengthAndCrcs logicalFileSectionLengthAndCrc) {
        this.logicalFileSectionLengthAndCrc = logicalFileSectionLengthAndCrc;
    }

    public @Nullable LengthAndCrcs getLogicalFileMetadataSectionLengthAndCrc() {
        return logicalFileMetadataSectionLengthAndCrc;
    }

    public void setLogicalFileMetadataSectionLengthAndCrc(LengthAndCrcs logicalFileMetadataSectionLengthAndCrc) {
        this.logicalFileMetadataSectionLengthAndCrc = logicalFileMetadataSectionLengthAndCrc;
    }

    public @Nullable byte[] getLogicalFileSectionKey() {
        return logicalFileSectionKey;
    }

    public void setLogicalFileSectionKey(byte[] logicalFileSectionKey) {
        this.logicalFileSectionKey = logicalFileSectionKey;
    }

    @Override
    public String toString() {
        return "File: " + file.getAbsolutePath() + " | Relative Path: " + relativePath + " | Size: " + file.length() + " bytes";
    }

    public static String getRelativePath(File file, String rootPath) {
        String absolutePath = file.getAbsolutePath();
        return absolutePath.replace(rootPath + File.separator, ""); // Remove root path
    }
}
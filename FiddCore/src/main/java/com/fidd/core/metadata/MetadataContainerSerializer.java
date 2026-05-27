package com.fidd.core.metadata;

import com.fidd.core.NamedEntry;

public interface MetadataContainerSerializer extends NamedEntry {
    byte[] serialize(MetadataContainer metadata);
    MetadataContainer deserialize(byte[] metadataBytes) throws NotEnoughBytesException;
}
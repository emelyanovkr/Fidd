package com.fidd.core.common;

import com.fidd.base.BaseRepositories;
import com.fidd.connectors.FiddConnector;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.metadata.MetadataContainer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FiddFileMetadataUtilTest {

    BaseRepositories baseRepositories;
    FiddConnector fiddConnector;
    FiddKey.Section section;
    MetadataContainerSerializer metadataContainerSerializer;
    FiddFileMetadataSerializer fiddFileMetadataSerializer;
    EncryptionAlgorithm encryptionAlgorithm;

    @BeforeEach
    void setup() {
        baseRepositories = mock(BaseRepositories.class, RETURNS_DEEP_STUBS);
        fiddConnector = mock(FiddConnector.class);
        section = mock(FiddKey.Section.class);
        metadataContainerSerializer = mock(MetadataContainerSerializer.class);
        fiddFileMetadataSerializer = mock(FiddFileMetadataSerializer.class);
        encryptionAlgorithm = mock(EncryptionAlgorithm.class);
    }

    @Test
    void testLoadFiddFileMetadata_success() throws Exception {
        long messageNumber = 42L;

        byte[] encryptedBytes = "encrypted".getBytes();
        byte[] decryptedBytes = "decrypted".getBytes();
        byte[] metadataBytes = "metadata".getBytes();

        MetadataContainer metadataContainer = mock(MetadataContainer.class);
        when(metadataContainer.metadataFormat()).thenReturn("metaFmt");
        when(metadataContainer.metadata()).thenReturn(metadataBytes);

        FiddFileMetadata fiddFileMetadata = mock(FiddFileMetadata.class);

        when(section.sectionOffset()).thenReturn(0L);
        when(section.sectionLength()).thenReturn(100L);
        when(section.encryptionAlgorithm()).thenReturn("AES");
        when(section.encryptionKeyData()).thenReturn("key".getBytes());

        when(fiddConnector.getFiddMessageChunk(messageNumber, 0L, 100L))
                .thenReturn(new ByteArrayInputStream(encryptedBytes));

        when(baseRepositories.metadataContainerFormatRepo().get("json"))
                .thenReturn(metadataContainerSerializer);

        when(baseRepositories.encryptionAlgorithmRepo().get("AES"))
                .thenReturn(encryptionAlgorithm);

        when(encryptionAlgorithm.decrypt("key".getBytes(), encryptedBytes))
                .thenReturn(decryptedBytes);

        when(metadataContainerSerializer.deserialize(decryptedBytes))
                .thenReturn(metadataContainer);

        when(baseRepositories.fiddFileMetadataFormatRepo().get("metaFmt"))
                .thenReturn(fiddFileMetadataSerializer);

        when(fiddFileMetadataSerializer.deserialize(metadataBytes))
                .thenReturn(fiddFileMetadata);

        InputStream stream = new ByteArrayInputStream(encryptedBytes);
        Pair<FiddFileMetadata, MetadataContainer> result =
                FiddFileMetadataUtil.loadFiddFileMetadata(
                        baseRepositories, stream, section, "json"
                );

        assertEquals(fiddFileMetadata, result.getLeft());
        assertEquals(metadataContainer, result.getRight());
    }

    @Test
    void testLoadFiddFileMetadata_noEncryptionKey_usesEmptyKey() throws Exception {
        long messageNumber = 1L;

        byte[] rawBytes = "raw".getBytes();
        byte[] metadataBytes = "metadata".getBytes();

        MetadataContainer metadataContainer = mock(MetadataContainer.class);
        when(metadataContainer.metadataFormat()).thenReturn("fmt");
        when(metadataContainer.metadata()).thenReturn(metadataBytes);

        FiddFileMetadata fiddFileMetadata = mock(FiddFileMetadata.class);

        when(section.sectionOffset()).thenReturn(0L);
        when(section.sectionLength()).thenReturn(10L);
        when(section.encryptionAlgorithm()).thenReturn("NONE");
        when(section.encryptionKeyData()).thenReturn(null);

        when(fiddConnector.getFiddMessageChunk(messageNumber, 0L, 10L))
                .thenReturn(new ByteArrayInputStream(rawBytes));

        when(baseRepositories.metadataContainerFormatRepo().get("json"))
                .thenReturn(metadataContainerSerializer);

        when(baseRepositories.encryptionAlgorithmRepo().get("NONE"))
                .thenReturn(encryptionAlgorithm);

        when(encryptionAlgorithm.decrypt(new byte[]{}, rawBytes))
                .thenReturn(rawBytes);

        when(metadataContainerSerializer.deserialize(rawBytes))
                .thenReturn(metadataContainer);

        when(baseRepositories.fiddFileMetadataFormatRepo().get("fmt"))
                .thenReturn(fiddFileMetadataSerializer);

        when(fiddFileMetadataSerializer.deserialize(metadataBytes))
                .thenReturn(fiddFileMetadata);

        InputStream stream = new ByteArrayInputStream(rawBytes);
        Pair<FiddFileMetadata, MetadataContainer> result =
                FiddFileMetadataUtil.loadFiddFileMetadata(
                        baseRepositories, stream, section, "json"
                );

        assertEquals(fiddFileMetadata, result.getLeft());
        assertEquals(metadataContainer, result.getRight());
    }

    @Test
    void testLoadFiddFileMetadata_missingEncryptionAlgorithm_throws() throws Exception {
        when(baseRepositories.metadataContainerFormatRepo().get("json"))
                .thenReturn(metadataContainerSerializer);

        when(section.encryptionAlgorithm()).thenReturn("BAD");

        when(baseRepositories.encryptionAlgorithmRepo().get("BAD"))
                .thenReturn(null);

        InputStream stream = mock(InputStream.class);
        assertThrows(RuntimeException.class, () ->
                FiddFileMetadataUtil.loadFiddFileMetadata(
                        baseRepositories, stream, section, "json"
                )
        );
    }

    @Test
    void testLoadFiddFileMetadata_missingMetadataSerializer_throws() {
        when(baseRepositories.metadataContainerFormatRepo().get("json"))
                .thenReturn(null);

        InputStream stream = mock(InputStream.class);
        assertThrows(NullPointerException.class, () ->
                FiddFileMetadataUtil.loadFiddFileMetadata(
                        baseRepositories, stream, section, "json"
                )
        );
    }

    @Test
    void testLoadFiddFileMetadata_missingFiddFileMetadataSerializer_throws() throws Exception {
        byte[] bytes = "abc".getBytes();

        MetadataContainer metadataContainer = mock(MetadataContainer.class);
        when(metadataContainer.metadataFormat()).thenReturn("fmt");

        when(baseRepositories.metadataContainerFormatRepo().get("json"))
                .thenReturn(metadataContainerSerializer);

        when(section.sectionOffset()).thenReturn(0L);
        when(section.sectionLength()).thenReturn(3L);
        when(section.encryptionAlgorithm()).thenReturn("AES");
        when(section.encryptionKeyData()).thenReturn(null);

        when(fiddConnector.getFiddMessageChunk(1L, 0L, 3L))
                .thenReturn(new ByteArrayInputStream(bytes));

        when(baseRepositories.encryptionAlgorithmRepo().get("AES"))
                .thenReturn(encryptionAlgorithm);

        when(metadataContainerSerializer.deserialize(bytes))
                .thenReturn(metadataContainer);

        when(baseRepositories.fiddFileMetadataFormatRepo().get("fmt"))
                .thenReturn(null);

        InputStream stream = new ByteArrayInputStream(bytes);
        assertThrows(RuntimeException.class, () ->
                FiddFileMetadataUtil.loadFiddFileMetadata(
                        baseRepositories, stream, section, "json"
                )
        );
    }

    @Test
    void testLoadFiddFileMetadata_propagatesIOException() throws Exception {
        when(baseRepositories.metadataContainerFormatRepo().get("json"))
                .thenReturn(metadataContainerSerializer);

        when(section.sectionOffset()).thenReturn(0L);
        when(section.sectionLength()).thenReturn(10L);
        when(section.encryptionAlgorithm()).thenReturn("AES");

        InputStream stream = mock(InputStream.class);
        when(stream.readAllBytes()).thenThrow(new IOException("boom"));

        assertThrows(IOException.class, () ->
                FiddFileMetadataUtil.loadFiddFileMetadata(
                        baseRepositories, stream, section, "json"
                )
        );
    }
}

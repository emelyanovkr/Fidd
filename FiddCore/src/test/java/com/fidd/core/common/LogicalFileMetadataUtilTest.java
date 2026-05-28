package com.fidd.core.common;

import com.fidd.base.BaseRepositories;
import com.fidd.base.Repository;
import com.fidd.connectors.FiddConnector;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import com.fidd.core.logicalfile.LogicalFileMetadataSerializer;
import com.fidd.core.metadata.MetadataContainer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import com.fidd.core.metadata.NotEnoughBytesException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogicalFileMetadataUtilTest {

    @Test
    void testGetLogicalFileMetadata_successfulFlow() throws Exception {
        // --- Mocks ---
        BaseRepositories repos = mock(BaseRepositories.class);
        FiddConnector connector = mock(FiddConnector.class);
        EncryptionAlgorithm encryption = mock(EncryptionAlgorithm.class);
        MetadataContainerSerializer containerSerializer = mock(MetadataContainerSerializer.class);
        LogicalFileMetadataSerializer metadataSerializer = mock(LogicalFileMetadataSerializer.class);

        // Section
        FiddKey.SectionWithHeader section = mock(FiddKey.SectionWithHeader.class);
        when(section.headerOffset()).thenReturn(0L);
        when(section.headerLength()).thenReturn(10);
        when(section.encryptionAlgorithm()).thenReturn("AES");
        when(section.encryptionKeyData()).thenReturn(new byte[]{1, 2, 3});

        // Repositories
        Repository<EncryptionAlgorithm> encryptionAlgorithmRepo = mock(Repository.class);
        when(repos.encryptionAlgorithmRepo()).thenReturn(encryptionAlgorithmRepo);
        Repository<MetadataContainerSerializer> metadataContainerFormatRepo = mock(Repository.class);
        when(repos.metadataContainerFormatRepo()).thenReturn(metadataContainerFormatRepo);

        when(encryptionAlgorithmRepo.get("AES")).thenReturn(encryption);
        when(metadataContainerFormatRepo.get("BLOBS")).thenReturn(containerSerializer);

        // Metadata container
        MetadataContainer container = mock(MetadataContainer.class);
        when(container.metadataFormat()).thenReturn("FMT");
        when(container.metadata()).thenReturn(new byte[]{9, 9});

        when(containerSerializer.deserialize(any())).thenReturn(container);

        // LogicalFileMetadata
        LogicalFileMetadata metadata = mock(LogicalFileMetadata.class);
        Repository<LogicalFileMetadataSerializer> logicalFileMetadataFormatRepo = mock(Repository.class);
        when(repos.logicalFileMetadataFormatRepo()).thenReturn(logicalFileMetadataFormatRepo);
        when(logicalFileMetadataFormatRepo.get("FMT")).thenReturn(metadataSerializer);
        when(metadataSerializer.deserialize(any())).thenReturn(metadata);

        // Input stream
        byte[] encryptedBytes = {10, 20, 30, 40, 50};
        InputStream stream = new ByteArrayInputStream(encryptedBytes);
        when(connector.getFiddMessageChunk(anyLong(), anyLong(), anyLong()))
                .thenReturn(stream);

        // Decrypt → just write plaintext directly
        doAnswer(invocation -> {
            InputStream in = invocation.getArgument(1);
            var out = invocation.getArgument(2, java.io.ByteArrayOutputStream.class);
            out.write(in.readAllBytes());
            return null;
        }).when(encryption).decrypt(any(), any(), any(), anyBoolean());

        // --- Execute ---
        Pair<LogicalFileMetadata, MetadataContainer> result =
                LogicalFileMetadataUtil.getLogicalFileMetadata(
                        repos, stream, section
                );

        // --- Verify ---
        assertNotNull(result);
        assertEquals(metadata, result.getLeft());
        assertEquals(container, result.getRight());
    }

    @Test
    void testGetLogicalFileMetadata_notEnoughBytes_thenSuccess() throws Exception {
        BaseRepositories repos = mock(BaseRepositories.class);
        FiddConnector connector = mock(FiddConnector.class);
        EncryptionAlgorithm encryption = mock(EncryptionAlgorithm.class);
        MetadataContainerSerializer containerSerializer = mock(MetadataContainerSerializer.class);

        FiddKey.SectionWithHeader section = mock(FiddKey.SectionWithHeader.class);
        when(section.headerOffset()).thenReturn(0L);
        when(section.headerLength()).thenReturn(10000);
        when(section.encryptionAlgorithm()).thenReturn("AES");
        when(section.encryptionKeyData()).thenReturn(new byte[]{1});

        Repository<EncryptionAlgorithm> encryptionAlgorithmRepo = mock(Repository.class);
        when(repos.encryptionAlgorithmRepo()).thenReturn(encryptionAlgorithmRepo);
        Repository<MetadataContainerSerializer> metadataContainerFormatRepo = mock(Repository.class);
        when(repos.metadataContainerFormatRepo()).thenReturn(metadataContainerFormatRepo);

        when(encryptionAlgorithmRepo.get("AES")).thenReturn(encryption);
        when(metadataContainerFormatRepo.get("BLOBS")).thenReturn(containerSerializer);

        // First call throws NotEnoughBytesException, second succeeds
        when(containerSerializer.deserialize(any()))
                .thenThrow(new NotEnoughBytesException())
                .thenReturn(mock(MetadataContainer.class));

        // LogicalFileMetadataSerializer
        LogicalFileMetadataSerializer metadataSerializer = mock(LogicalFileMetadataSerializer.class);
        Repository<LogicalFileMetadataSerializer> logicalFileMetadataFormatRepo = mock(Repository.class);
        when(repos.logicalFileMetadataFormatRepo()).thenReturn(logicalFileMetadataFormatRepo);
        when(logicalFileMetadataFormatRepo.get(any())).thenReturn(metadataSerializer);
        when(metadataSerializer.deserialize(any())).thenReturn(mock(LogicalFileMetadata.class));

        // Input stream
        InputStream stream = new InputStream() {
            @Override
            public int read() {
                return (int)(Math.random()*256);
            }
        };
        when(connector.getFiddMessageChunk(anyLong(), anyLong(), anyLong()))
                .thenReturn(stream);

        // Decrypt passthrough
        doAnswer(invocation -> {
            InputStream in = invocation.getArgument(1);
            var out = invocation.getArgument(2, java.io.ByteArrayOutputStream.class);
            out.write(in.readAllBytes());
            return null;
        }).when(encryption).decrypt(any(), any(), any(), anyBoolean());

        var result = LogicalFileMetadataUtil.getLogicalFileMetadata(
                repos, stream, section
        );

        assertNotNull(result);
    }

    @Test
    void testGetLogicalFileMetadata_unsupportedEncryptionAlgorithm() {
        BaseRepositories repos = mock(BaseRepositories.class);
        FiddConnector connector = mock(FiddConnector.class);
        FiddKey.SectionWithHeader section = mock(FiddKey.SectionWithHeader.class);

        when(section.encryptionAlgorithm()).thenReturn("BAD");
        Repository<EncryptionAlgorithm> encryptionAlgorithmRepo = mock(Repository.class);
        when(repos.encryptionAlgorithmRepo()).thenReturn(encryptionAlgorithmRepo);
        when(encryptionAlgorithmRepo.get("BAD")).thenReturn(null);

        assertThrows(RuntimeException.class, () ->
                LogicalFileMetadataUtil.getLogicalFileMetadata(
                        repos, mock(InputStream.class), section
                )
        );
    }

    @Test
    void testGetLogicalFileMetadata_unsupportedMetadataFormat() throws Exception {
        BaseRepositories repos = mock(BaseRepositories.class);
        FiddConnector connector = mock(FiddConnector.class);
        EncryptionAlgorithm encryption = mock(EncryptionAlgorithm.class);
        MetadataContainerSerializer containerSerializer = mock(MetadataContainerSerializer.class);

        FiddKey.SectionWithHeader section = mock(FiddKey.SectionWithHeader.class);
        when(section.headerOffset()).thenReturn(0L);
        when(section.headerLength()).thenReturn(5);
        when(section.encryptionAlgorithm()).thenReturn("AES");
        when(section.encryptionKeyData()).thenReturn(new byte[]{1});

        Repository<EncryptionAlgorithm> encryptionAlgorithmRepo = mock(Repository.class);
        when(repos.encryptionAlgorithmRepo()).thenReturn(encryptionAlgorithmRepo);
        Repository<MetadataContainerSerializer> metadataContainerFormatRepo = mock(Repository.class);
        when(repos.metadataContainerFormatRepo()).thenReturn(metadataContainerFormatRepo);

        when(encryptionAlgorithmRepo.get("AES")).thenReturn(encryption);
        when(metadataContainerFormatRepo.get("BLOBS")).thenReturn(containerSerializer);

        MetadataContainer container = mock(MetadataContainer.class);
        when(container.metadataFormat()).thenReturn("UNKNOWN");

        when(containerSerializer.deserialize(any())).thenReturn(container);

        Repository<LogicalFileMetadataSerializer> logicalFileMetadataFormatRepo = mock(Repository.class);
        when(logicalFileMetadataFormatRepo.get("UNKNOWN")).thenReturn(null);

        InputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(connector.getFiddMessageChunk(anyLong(), anyLong(), anyLong()))
                .thenReturn(stream);

        doAnswer(invocation -> {
            InputStream in = invocation.getArgument(1);
            var out = invocation.getArgument(2, java.io.ByteArrayOutputStream.class);
            out.write(in.readAllBytes());
            return null;
        }).when(encryption).decrypt(any(), any(), any(), anyBoolean());

        assertThrows(RuntimeException.class, () ->
                LogicalFileMetadataUtil.getLogicalFileMetadata(
                        repos, encryption, stream, section, containerSerializer, true
                )
        );
    }

    @Test
    void testConcat() {
        byte[] a = {1, 2};
        byte[] b = {3, 4, 5};
        byte[] result = LogicalFileMetadataUtil.concat(a, b, 3);

        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void testWarnAndMaybeThrow_noThrow() {
        assertDoesNotThrow(() ->
                LogicalFileMetadataUtil.warnAndMaybeThrow("msg", false)
        );
    }

    @Test
    void testWarnAndMaybeThrow_withThrow() {
        assertThrows(RuntimeException.class, () ->
                LogicalFileMetadataUtil.warnAndMaybeThrow("msg", true)
        );
    }
}

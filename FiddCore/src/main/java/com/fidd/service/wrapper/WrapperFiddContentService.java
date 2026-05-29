package com.fidd.service.wrapper;

import com.fidd.base.BaseRepositories;
import com.fidd.connectors.FiddConnector;
import com.fidd.core.common.FiddKeyUtil;
import com.fidd.core.common.LogicalFileMetadataUtil;
import com.fidd.core.common.SubInputStream;
import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import com.fidd.core.metadata.FiddMetadatas;
import com.fidd.core.metadata.MetadataContainer;
import com.fidd.service.FiddContentService;
import com.fidd.service.LogicalFileInfo;
import com.google.common.base.Supplier;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.fidd.core.common.FiddFileMetadataUtil.loadFiddFileMetadata;
import static com.fidd.core.common.LogicalFileUtil.getLogicalFileInputStream;
import static com.fidd.core.common.LogicalFileUtil.getLogicalFileInputStreamChunk;
import static com.google.common.base.Preconditions.checkNotNull;

public class WrapperFiddContentService implements FiddContentService {
    public final static Logger LOGGER = LoggerFactory.getLogger(WrapperFiddContentService.class);

    // TODO: hardcoding this to "BLOBS" for now
    final static String METADATA_CONTAINER_SERIALIZER_FORMAT = "BLOBS";

    protected final BaseRepositories baseRepositories;
    protected final FiddConnector fiddConnector;
    protected final Supplier<Pair<X509Certificate, PrivateKey>> keySupplier;

    public WrapperFiddContentService(BaseRepositories baseRepositories, FiddConnector fiddConnector,
                                     Supplier<Pair<X509Certificate, PrivateKey>> keySupplier) {
        this.baseRepositories = baseRepositories;
        this.fiddConnector = fiddConnector;
        this.keySupplier = keySupplier;
    }

    @Override
    public List<Long> getMessageNumbersTail(int count) {
        return fiddConnector.getMessageNumbersTail(count);
    }

    @Override
    public List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive) {
        return fiddConnector.getMessageNumbersBefore(messageNumber, count, inclusive);
    }

    @Override
    public List<Long> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest,
                                               long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest) {
        return fiddConnector.getMessageNumbersBetween(latestMessage, inclusiveLatest, earliestMessage,
                inclusiveEarliest, count, getLatest);
    }

    protected @Nullable FiddKey loadFiddKey(long messageNumber) throws Exception {
        LOGGER.info("Loading FiddKey for message #" + messageNumber);
        byte[] fiddKeyBytes = null;
        Pair<X509Certificate, PrivateKey> pair = keySupplier.get();
        if (pair != null) {
            fiddKeyBytes = FiddKeyUtil.loadFiddKeyBytes(baseRepositories, messageNumber, fiddConnector,
                    pair.getLeft(), checkNotNull(pair.getRight()));
        }
        if (fiddKeyBytes == null) {
            fiddKeyBytes = FiddKeyUtil.loadDefaultFiddKeyBytes(messageNumber, fiddConnector);
        }
        if (fiddKeyBytes == null) { return null; }
        return FiddKeyUtil.loadFiddKeyFromBytes(baseRepositories, fiddKeyBytes);
    }

    @Override
    public @Nullable FiddMetadatas getFiddMetadatas(long messageNumber) {
        try {
            LOGGER.info("Getting FiddFileMetadata for message #" + messageNumber);

            // 1. Load FiddKey
            FiddKey fiddKey = loadFiddKey(messageNumber);
            if (fiddKey == null) { return null; }

            // 2. Form chunk list and load chunkStream
            List<FiddConnector.Chunk<FiddKey.Section>> chunks = new ArrayList<>();

            FiddKey.Section fiddFileMetadataSection = fiddKey.fiddFileMetadata();
            chunks.add(new FiddConnector.Chunk<>(fiddFileMetadataSection.sectionOffset(), fiddFileMetadataSection.sectionLength(), fiddFileMetadataSection));

            for (int i = 0; i < fiddKey.logicalFiles().size(); i++) {
                FiddKey.SectionWithHeader logicalFileSection = fiddKey.logicalFiles().get(i);
                chunks.add(new FiddConnector.Chunk<>(logicalFileSection.headerOffset(), logicalFileSection.headerLength(), logicalFileSection));
            }

            chunks.sort(Comparator.comparingLong(FiddConnector.Chunk::offset));

            /* TODO: concatenatedChunkStream is only closed indirectly by the last SubInputStream (via closeParent=true).
                If an exception happens before processing the last chunk, the underlying stream will remain open (resource leak). */
            InputStream concatenatedChunkStream = fiddConnector.getFiddMessageChunks(messageNumber, chunks);

            // 3. Load metadata Sections
            Pair<FiddFileMetadata, MetadataContainer> fiddFileMetadataAndContainer = null;
            List<LogicalFileInfo> logicalFileInfo = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                FiddConnector.Chunk<FiddKey.Section> chunk = chunks.get(i);

                SubInputStream subStream = new SubInputStream(concatenatedChunkStream, 0, chunk.length(), i == chunks.size()-1);
                if (chunk.info() instanceof FiddKey.SectionWithHeader logicalFileSection) {
                    // Logical file section
                    LOGGER.info("Getting LogicalFileMetadata for Section message #" + messageNumber);
                    Pair<LogicalFileMetadata, MetadataContainer> logicalFileMetadataAndContainer =
                            LogicalFileMetadataUtil.getLogicalFileMetadata(baseRepositories, subStream,
                                    logicalFileSection);
                    logicalFileInfo.add(LogicalFileInfo.of(checkNotNull(logicalFileMetadataAndContainer).getLeft(),
                            logicalFileSection
                    ));
                } else {
                    // Fidd metadata section
                    LOGGER.info("Getting FiddFileMetadata for message #" + messageNumber);
                    fiddFileMetadataAndContainer = loadFiddFileMetadata(baseRepositories, subStream,
                            fiddFileMetadataSection, METADATA_CONTAINER_SERIALIZER_FORMAT);
                }
            }

            return FiddMetadatas.of(checkNotNull(fiddFileMetadataAndContainer).getLeft(), logicalFileInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @Nullable InputStream readLogicalFile(long messageNumber, LogicalFileInfo LogicalFileInfo) {
        try {
            LOGGER.info("Getting LogicalFile " + messageNumber + " / " + LogicalFileInfo.metadata().filePath());
            return getLogicalFileInputStream(baseRepositories, fiddConnector,
                    messageNumber, LogicalFileInfo.section());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @Nullable InputStream readLogicalFileChunk(long messageNumber, LogicalFileInfo LogicalFileInfo, long offset, long length) {
        try {
            LOGGER.info("Getting LogicalFileChunk " + messageNumber + " / " + LogicalFileInfo.metadata().filePath() +
                    " from: " + offset + " size: " + length);
            return getLogicalFileInputStreamChunk(baseRepositories, fiddConnector,
                    messageNumber, LogicalFileInfo.section(), offset, length);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

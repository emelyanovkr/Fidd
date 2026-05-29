package com.fidd.cache;

import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.cache.ram.ChunkCachingInputStream;
import com.fidd.connectors.cache.ram.ChunkKey;
import com.fidd.connectors.cache.ram.MessageChunkCache;
import com.fidd.connectors.cache.ram.MessageKey;
import com.fidd.connectors.cache.ram.RamCache;
import com.fidd.core.fiddkey.FiddKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ChunkCachingInputStreamTest {

    private RamCache ramCache;
    private final String fiddId = "testFidd";
    private final long messageNumber = 42L;

    @BeforeEach
    void setUp() {
        ramCache = new RamCache(10, 10, 10, 10);
    }

    private FiddConnector.Chunk<FiddKey.Section> createChunk(long offset, long length) {
        return new FiddConnector.Chunk<>(offset, length, null);
    }

    @Test
    void testReadByteByByteAndCache() throws IOException {
        byte[] data1 = new byte[]{1, 2, 3};
        byte[] data2 = new byte[]{4, 5};
        byte[] data3 = new byte[]{6, 7, 8, 9};

        byte[] allData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};

        List<FiddConnector.Chunk<FiddKey.Section>> chunks = Arrays.asList(
                createChunk(0, 3),
                createChunk(3, 2),
                createChunk(5, 4)
        );

        ByteArrayInputStream bais = new ByteArrayInputStream(allData);
        ChunkCachingInputStream stream = new ChunkCachingInputStream(
                fiddId, messageNumber, ramCache, 10, bais, chunks);

        // Read byte by byte
        for (int i = 0; i < allData.length; i++) {
            assertEquals(allData[i], (byte) stream.read());
        }
        assertEquals(-1, stream.read()); // EOF

        // Check if cached
        MessageChunkCache cache = ramCache.messageChunkCache.getIfPresent(new MessageKey(fiddId, messageNumber));
        assertNotNull(cache);

        assertArrayEquals(data1, cache.get(new ChunkKey(0, 3)));
        assertArrayEquals(data2, cache.get(new ChunkKey(3, 2)));
        assertArrayEquals(data3, cache.get(new ChunkKey(5, 4)));
    }

    @Test
    void testReadWithBufferAndCache() throws IOException {
        byte[] data1 = new byte[]{1, 2, 3};
        byte[] data2 = new byte[]{4, 5};
        byte[] data3 = new byte[]{6, 7, 8, 9};

        byte[] allData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};

        List<FiddConnector.Chunk<FiddKey.Section>> chunks = Arrays.asList(
                createChunk(0, 3),
                createChunk(3, 2),
                createChunk(5, 4)
        );

        ByteArrayInputStream bais = new ByteArrayInputStream(allData);
        ChunkCachingInputStream stream = new ChunkCachingInputStream(
                fiddId, messageNumber, ramCache, 10, bais, chunks);

        byte[] buffer = new byte[5];

        int read1 = stream.read(buffer); // reads first 5 bytes
        assertEquals(5, read1);
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, buffer);

        // At this point, chunk1 and chunk2 should be cached
        MessageChunkCache cache = ramCache.messageChunkCache.getIfPresent(new MessageKey(fiddId, messageNumber));
        assertNotNull(cache);
        assertArrayEquals(data1, cache.get(new ChunkKey(0, 3)));
        assertArrayEquals(data2, cache.get(new ChunkKey(3, 2)));
        assertNull(cache.get(new ChunkKey(5, 4)));

        int read2 = stream.read(buffer); // reads remaining 4 bytes
        assertEquals(4, read2);
        assertArrayEquals(new byte[]{6, 7, 8, 9}, Arrays.copyOf(buffer, 4));

        assertEquals(-1, stream.read(buffer)); // EOF

        assertArrayEquals(data3, cache.get(new ChunkKey(5, 4)));
    }

    @Test
    void testOversizedChunksAreNotCached() throws IOException {
        byte[] allData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9};

        List<FiddConnector.Chunk<FiddKey.Section>> chunks = Arrays.asList(
                createChunk(0, 5), // > 4, will not be cached
                createChunk(5, 4)  // <= 4, will be cached
        );

        ByteArrayInputStream bais = new ByteArrayInputStream(allData);
        // maxChunkSize is 4, so chunk 0 will NOT be cached, chunk 1 WILL be cached
        ChunkCachingInputStream stream = new ChunkCachingInputStream(
                fiddId, messageNumber, ramCache, 4, bais, chunks);

        byte[] buffer = new byte[10];
        stream.read(buffer);

        MessageChunkCache cache = ramCache.messageChunkCache.getIfPresent(new MessageKey(fiddId, messageNumber));
        assertNotNull(cache);
        assertNull(cache.get(new ChunkKey(0, 5)));
        assertArrayEquals(new byte[]{6, 7, 8, 9}, cache.get(new ChunkKey(5, 4)));
    }
}

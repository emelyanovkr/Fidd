package com.fidd.connectors.cache.ram;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class MessageChunkCache {
    private final int maxSizeBytes;
    private int currentSizeBytes = 0;

    public MessageChunkCache(int maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public MessageChunkCache() {
        this(100 * 1024);  // 100 KB hard limit
    }

    // LinkedHashMap in access-order mode (LRU)
    private final Map<ChunkKey, byte[]> lruMap = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ChunkKey, byte[]> eldest) {
            // Evict until we are under the limit
            if (currentSizeBytes > maxSizeBytes) {
                currentSizeBytes -= eldest.getValue().length;
                return true;
            }
            return false;
        }
    };

    // Thread-safe access for concurrent requests to the SAME message
    public synchronized void put(ChunkKey chunkId, byte[] chunk) {
        // If updating an existing chunk, adjust size first
        byte[] existing = lruMap.get(chunkId);
        if (existing != null) {
            currentSizeBytes -= existing.length;
        }

        currentSizeBytes += chunk.length;
        lruMap.put(chunkId, chunk);

        // LinkedHashMap's removeEldestEntry will automatically be called here
        // Note: If a single chunk is > 100KB, you might need a while loop in removeEldestEntry
        // to evict multiple items, but typically chunks are fixed size.
    }

    @Nullable
    public synchronized byte[] get(ChunkKey chunkId) {
        return lruMap.get(chunkId); // automatically updates LRU access order
    }
}
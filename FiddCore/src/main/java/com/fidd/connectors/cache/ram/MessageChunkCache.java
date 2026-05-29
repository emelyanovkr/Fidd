package com.fidd.connectors.cache.ram;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class MessageChunkCache {
    public static final int MAX_CHUNK_SIZE = 100*1024;

    private final int maxSizeBytes;
    private int currentSizeBytes = 0;

    public MessageChunkCache(int maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public MessageChunkCache() {
        this(MAX_CHUNK_SIZE);  // 100 KB hard limit
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

        // Evict as many eldest entries as needed to get back under the limit
        while (currentSizeBytes > maxSizeBytes && !lruMap.isEmpty()) {
            Map.Entry<ChunkKey, byte[]> eldest = lruMap.entrySet().iterator().next();
            currentSizeBytes -= eldest.getValue().length;
            lruMap.remove(eldest.getKey());
        }
    }

    @Nullable
    public synchronized byte[] get(ChunkKey chunkId) {
        return lruMap.get(chunkId); // automatically updates LRU access order
    }
}
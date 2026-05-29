package com.fidd.connectors.cache.ram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageChunkCacheTest {

    static ChunkKey s(int i) {
        return new ChunkKey(i,i);
    }

    private MessageChunkCache cache;

    @BeforeEach
    void setUp() {
        cache = new MessageChunkCache();
    }

    @Test
    void testPutAndGet() {
        byte[] data = new byte[]{1, 2, 3};
        cache.put(s(1), data);
        assertArrayEquals(data, cache.get(s(1)));
    }

    @Test
    void testEvictionWhenExceedingLimit() {
        // max size is 100 * 1024 = 102400 bytes.
        // Let's insert 10 chunks of 10 * 1024 = 102400 bytes total.
        byte[] chunk10k = new byte[10 * 1024];

        for (int i = 0; i < 10; i++) {
            cache.put(s(i), chunk10k);
        }

        // All should be there
        for (int i = 0; i < 10; i++) {
            assertNotNull(cache.get(s(i)), "Chunk " + i + " should be present");
        }

        // Now put one more, it should evict the first one (id 0)
        cache.put(s(10), chunk10k);

        assertNull(cache.get(s(0)), "Chunk 0 should be evicted");
        assertNotNull(cache.get(s(1)), "Chunk 1 should still be present");
        assertNotNull(cache.get(s(10)), "Chunk 10 should be present");
    }

    @Test
    void testLruAccessOrder() {
        byte[] chunk10k = new byte[10 * 1024];
        for (int i = 0; i < 10; i++) {
            cache.put(s(i), chunk10k);
        }

        // Access chunk 0 to make it recently used
        cache.get(s(0));

        // Put 10th item (11th total), should evict chunk 1 since 0 was recently accessed
        cache.put(s(10), chunk10k);

        assertNotNull(cache.get(s(0)), "Chunk 0 should NOT be evicted because it was accessed");
        assertNull(cache.get(s(1)), "Chunk 1 should be evicted instead");
    }

    @Test
    void testReplaceExistingAdjustsSizeCorrectly() {
        byte[] chunk50k = new byte[50 * 1024];
        cache.put(s(1), chunk50k);
        cache.put(s(2), chunk50k);
        // Size is now 100k. Max is 100k.

        // Replace chunk 2 with a smaller chunk
        byte[] chunk10k = new byte[10 * 1024];
        cache.put(s(2), chunk10k);
        // Size should now be 60k.

        // Put another 30k chunk. Total = 90k, no eviction.
        byte[] chunk30k = new byte[30 * 1024];
        cache.put(s(3), chunk30k);

        assertNotNull(cache.get(s(1)));
        assertNotNull(cache.get(s(2)));
        assertNotNull(cache.get(s(3)));

        // Put another 30k chunk. Total would be 120k. Should evict chunk 1 (50k).
        // Resulting total = 70k.
        cache.put(s(4), chunk30k);

        assertNull(cache.get(s(1)), "Chunk 1 should be evicted");
        assertNotNull(cache.get(s(2)));
        assertNotNull(cache.get(s(3)));
        assertNotNull(cache.get(s(4)));
    }

    @Test
    void testEvictManyEldestEntries() {
        byte[] chunk10k = new byte[10 * 1024];
        for (int i = 0; i < 10; i++) {
            cache.put(s(i), chunk10k);
        }

        // Cache is now full (100k).
        // Put a larger chunk of 35k. The cache will exceed limit.
        // It should evict multiple 10k chunks (0, 1, 2, 3) to fall back <= 100k.
        byte[] chunk35k = new byte[35 * 1024];
        cache.put(s(10), chunk35k);

        assertNull(cache.get(s(0)), "Chunk 0 should be evicted");
        assertNull(cache.get(s(1)), "Chunk 1 should be evicted");
        assertNull(cache.get(s(2)), "Chunk 2 should be evicted");
        assertNull(cache.get(s(3)), "Chunk 3 should be evicted");

        // Chunk 4 should remain
        assertNotNull(cache.get(s(4)), "Chunk 4 should remain");
        assertNotNull(cache.get(s(10)), "Chunk 10 should be present");
    }
}

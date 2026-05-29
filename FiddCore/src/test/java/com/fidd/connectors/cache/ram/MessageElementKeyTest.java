package com.fidd.connectors.cache.ram;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class MessageElementKeyTest {

    @Test
    public void testEqualsAndHashCode() {
        byte[] data1 = new byte[]{1, 2, 3};
        byte[] data2 = new byte[]{1, 2, 3};
        byte[] data3 = new byte[]{1, 2, 4}; // Different data

        // Ensure we are working with different array instances
        assertNotSame(data1, data2);

        MessageElementKey key1 = new MessageElementKey("fidd-1", 100L, data1);
        MessageElementKey key2 = new MessageElementKey("fidd-1", 100L, data2);
        MessageElementKey key3 = new MessageElementKey("fidd-1", 100L, data3); // Different byte[] content
        MessageElementKey key4 = new MessageElementKey("fidd-2", 100L, data1); // Different fiddId
        MessageElementKey key5 = new MessageElementKey("fidd-1", 101L, data1); // Different messageNumber

        // Equality check
        assertEquals(key1, key2, "Keys with equivalent content but different byte array objects should be equal");
        assertEquals(key1.hashCode(), key2.hashCode(), "HashCodes should be identical for equivalent keys");

        // Inequality check
        assertNotEquals(key1, key3, "Keys with different byte array content should not be equal");
        assertNotEquals(key1, key4, "Keys with different fiddId should not be equal");
        assertNotEquals(key1, key5, "Keys with different messageNumber should not be equal");
    }

    @Test
    public void testHashMapUsage() {
        byte[] data1 = new byte[]{4, 5, 6};
        byte[] data2 = new byte[]{4, 5, 6};

        MessageElementKey key1 = new MessageElementKey("fidd-test", 42L, data1);
        MessageElementKey key2 = new MessageElementKey("fidd-test", 42L, data2);

        Map<MessageElementKey, String> map = new HashMap<>();
        map.put(key1, "Cached Value");

        // key2 should successfully resolve the value put by key1
        assertEquals("Cached Value", map.get(key2), "HashMap get() should find the entry using an equivalent key");
    }
}


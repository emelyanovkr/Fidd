package com.fidd.connectors.cache.ram;

import java.util.Arrays;
import java.util.Objects;

public record MessageElementKey(String fiddId, long messageNumber, byte[] element) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageElementKey that)) return false;
        return messageNumber == that.messageNumber &&
               Objects.equals(fiddId, that.fiddId) &&
               Arrays.equals(element, that.element);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(fiddId);
        result = 31 * result + Long.hashCode(messageNumber);
        result = 31 * result + Arrays.hashCode(element);
        return result;
    }
}

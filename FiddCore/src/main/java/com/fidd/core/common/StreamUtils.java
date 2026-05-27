package com.fidd.core.common;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {
    public static void skipAll(InputStream stream, long n) throws IOException {
        long remaining = n;
        while (remaining > 0) {
            long skipped = stream.skip(remaining);
            if (skipped <= 0) {
                // If skip() returns 0, try reading and discarding one byte
                if (stream.read() == -1) {
                    throw new EOFException("Reached end of stream before skipping " + n + " bytes");
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }
}

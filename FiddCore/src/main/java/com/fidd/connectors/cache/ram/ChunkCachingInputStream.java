package com.fidd.connectors.cache.ram;

import com.fidd.connectors.FiddConnector;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.Nullable;

public class ChunkCachingInputStream extends InputStream {
    final String fiddId;
    final long messageNumber;
    final RamCache ramCache;

    final long maxChunkSize;

    final InputStream in;
    final List<? extends FiddConnector.Chunk<?>> chunks;

    int currentChunkIndex = 0;
    long bytesReadInCurrentChunk = 0;
    @Nullable byte[] currentChunkBuffer = null;

    public ChunkCachingInputStream(String fiddId,
                                   long messageNumber,
                                   RamCache ramCache,

                                   long maxChunkSize,

                                   InputStream in,
                                   List<? extends FiddConnector.Chunk<?>> chunks) {
        this.fiddId = fiddId;
        this.messageNumber = messageNumber;
        this.ramCache = ramCache;

        this.maxChunkSize = maxChunkSize;

        this.in = in;
        this.chunks = chunks;

        if (!chunks.isEmpty()) {
            long len = chunks.get(0).length();
            if (len <= maxChunkSize) {
                currentChunkBuffer = new byte[(int) len];
            }
        }
    }

    private void recordReadByte(byte b) {
        if (currentChunkIndex >= chunks.size()) return;
        FiddConnector.Chunk<?> chunk = chunks.get(currentChunkIndex);
        if (currentChunkBuffer != null) {
            currentChunkBuffer[(int)bytesReadInCurrentChunk] = b;
        }
        bytesReadInCurrentChunk++;
        if (bytesReadInCurrentChunk == chunk.length()) {
            finishCurrentChunk(chunk);
        }
    }

    private void recordReadBytes(byte[] b, int off, int len) {
        int processed = 0;
        while (processed < len && currentChunkIndex < chunks.size()) {
            FiddConnector.Chunk<?> chunk = chunks.get(currentChunkIndex);
            long remainingInChunk = chunk.length() - bytesReadInCurrentChunk;
            int toProcess = (int) Math.min(len - processed, remainingInChunk);

            if (currentChunkBuffer != null) {
                System.arraycopy(b, off + processed, currentChunkBuffer, (int)bytesReadInCurrentChunk, toProcess);
            }

            bytesReadInCurrentChunk += toProcess;
            processed += toProcess;

            if (bytesReadInCurrentChunk == chunk.length()) {
                finishCurrentChunk(chunk);
            }
        }
    }

    private void finishCurrentChunk(FiddConnector.Chunk<?> chunk) {
        if (currentChunkBuffer != null) {
            cacheChunk(chunk, currentChunkBuffer);
        }
        currentChunkIndex++;
        bytesReadInCurrentChunk = 0;
        if (currentChunkIndex < chunks.size()) {
            long nextLen = chunks.get(currentChunkIndex).length();
            if (nextLen <= maxChunkSize) {
                currentChunkBuffer = new byte[(int) nextLen];
            } else {
                currentChunkBuffer = null; // don't cache large chunks
            }
        }
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b != -1) {
            recordReadByte((byte) b);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        if (read > 0) {
            recordReadBytes(b, off, read);
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    protected void cacheChunk(FiddConnector.Chunk<?> chunk, byte[] chunkBytes) {
        MessageChunkCache messageChunkCache = ramCache.messageChunkCache.get(
                new MessageKey(fiddId, messageNumber), k -> new MessageChunkCache());

        ChunkKey chunkKey = new ChunkKey(chunk.offset(), chunk.length());
        messageChunkCache.put(chunkKey, chunkBytes);
    }
}

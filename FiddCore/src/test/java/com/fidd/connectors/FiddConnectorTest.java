package com.fidd.connectors;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class FiddConnectorTest {

    @Test
    public void testGetFiddMessageChunks_Empty() throws Exception {
        FiddConnector connector = mock(FiddConnector.class);
        when(connector.getFiddMessageChunks(anyLong(), anyList())).thenCallRealMethod();

        try (InputStream is = connector.getFiddMessageChunks(1L, List.of())) {
            assertEquals(-1, is.read(), "Stream should be empty");
        }

        verify(connector, never()).getFiddMessageChunk(anyLong(), anyLong(), anyLong());
    }

    @Test
    public void testGetFiddMessageChunks_SingleChunk() throws Exception {
        FiddConnector connector = mock(FiddConnector.class);
        when(connector.getFiddMessageChunks(anyLong(), anyList())).thenCallRealMethod();

        when(connector.getFiddMessageChunk(1L, 10, 5))
                .thenReturn(new ByteArrayInputStream("single".getBytes()));

        List<FiddConnector.Chunk<Void>> chunks = List.of(
                new FiddConnector.Chunk<>(10, 5, null)
        );

        try (InputStream is = connector.getFiddMessageChunks(1L, chunks)) {
            byte[] bytes = is.readAllBytes();
            assertEquals("single", new String(bytes));
        }

        verify(connector, times(1)).getFiddMessageChunk(1L, 10, 5);
    }

    @Test
    public void testGetFiddMessageChunks_ContiguousAndGaps() throws Exception {
        FiddConnector connector = mock(FiddConnector.class);
        when(connector.getFiddMessageChunks(anyLong(), anyList())).thenCallRealMethod();

        // Setup mock inputs for the respective coalesced chunks
        when(connector.getFiddMessageChunk(42L, 0, 15))
                .thenReturn(new ByteArrayInputStream("contiguous_data".getBytes()));
        when(connector.getFiddMessageChunk(42L, 20, 3))
                .thenReturn(new ByteArrayInputStream("gap".getBytes()));
        when(connector.getFiddMessageChunk(42L, 25, 4))
                .thenReturn(new ByteArrayInputStream("tail".getBytes()));

        List<FiddConnector.Chunk<Void>> chunks = List.of(
                new FiddConnector.Chunk<>(0, 5, null),
                new FiddConnector.Chunk<>(5, 10, null), // Contiguous with previous
                new FiddConnector.Chunk<>(20, 3, null), // Gap
                new FiddConnector.Chunk<>(25, 2, null), // Gap
                new FiddConnector.Chunk<>(27, 2, null)  // Contiguous with previous
        );

        try (InputStream is = connector.getFiddMessageChunks(42L, chunks)) {
            byte[] bytes = is.readAllBytes();
            assertEquals("contiguous_datagaptail", new String(bytes));
        }

        // Verify exactly 3 calls with coalesced lengths
        InOrder inOrder = inOrder(connector);
        inOrder.verify(connector).getFiddMessageChunk(42L, 0, 15);
        inOrder.verify(connector).getFiddMessageChunk(42L, 20, 3);
        inOrder.verify(connector).getFiddMessageChunk(42L, 25, 4);
        verify(connector, never()).getFiddMessageChunk(eq(42L), eq(0L), eq(5L)); // Check individual segments weren't requested
    }
}


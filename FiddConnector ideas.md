```java
package com.fidd.connectors;

import java.io.InputStream;
import java.util.List;

public interface FastFiddConnector extends FiddConnector {
    interface Chunk {
        long offset();
        long length();
    }

    // TODO: Use case for length? This is not the "right" length anyway, it's message file size
    //  true length can only be calculated from FiddKey
    //interface MessageNumberAndLength {
    //    long messageNumber();
    //    long messageLength();
    //}
    /** Descending order */
    //List<MessageNumberAndLength> getMessageNumbersWithLengthsTail(int count);
    /** Descending order */
    //List<MessageNumberAndLength> getMessageNumbersWithLengthsBefore(long messageNumber, int count, boolean inclusive);
    /** Descending order */
    //List<MessageNumberAndLength> getMessageNumbersWithLengthsBetween(long latestMessage, boolean inclusiveLatest,
    //                                                      long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest);

    // TODO: Use case
    //interface PageResult<T> {
    //    List<T> items();
    //    int page();
    //    int pageSize();
    //    boolean last();
    //}
    //PageResult<byte[]> listFiddKeys(long messageNumber, byte[] footprint, int page);//Size according to the connector

    //PageResult<byte[]> getFiddKeySignatures(long messageNumber, int index, int page);
    //PageResult<byte[]> getFiddMessageSignatures(long messageNumber, int index, int page);
}
```

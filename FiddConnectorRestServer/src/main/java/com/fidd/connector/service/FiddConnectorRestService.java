package com.fidd.connector.service;

import com.fidd.connectors.FiddConnector;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FiddConnectorRestService {

  private final FiddConnector fiddConnector;

  public FiddConnectorRestService(FiddConnector fiddConnector) {
    this.fiddConnector = fiddConnector;
  }

  public List<Long> getMessageNumbersTail(int count) {
    return fiddConnector.getMessageNumbersTail(count);
  }

  public List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive) {
    return fiddConnector.getMessageNumbersBefore(messageNumber, count, inclusive);
  }

  public List<Long> getMessageNumbersBetween(
      long latestMessage,
      boolean inclusiveLatest,
      long earliestMessage,
      boolean inclusiveEarliest,
      int count,
      boolean getLatest) {
    return fiddConnector.getMessageNumbersBetween(
        latestMessage, inclusiveLatest, earliestMessage, inclusiveEarliest, count, getLatest);
  }

  public long getFiddMessageSize(long messageNumber) {
    return fiddConnector.getFiddMessageSize(messageNumber);
  }

  public InputStream getFiddMessageChunk(long messageNumber, long offset, long length) {
    return fiddConnector.getFiddMessageChunk(messageNumber, offset, length);
  }

  public List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprint)
      throws IOException {
    return fiddConnector.getFiddKeyCandidates(messageNumber, footprint);
  }

  public byte[] getFiddKey(long messageNumber, byte[] key) {
    return fiddConnector.getFiddKey(messageNumber, key);
  }

  public byte[] getUnencryptedFiddKey(long messageNumber) {
    return fiddConnector.getUnencryptedFiddKey(messageNumber);
  }

  public int getFiddKeySignatureCount(long messageNumber) {
    return fiddConnector.getFiddKeySignatureCount(messageNumber);
  }

  public byte[] getFiddKeySignature(long messageNumber, int index) {
    return fiddConnector.getFiddKeySignature(messageNumber, index);
  }

  public int getFiddMessageSignatureCount(long messageNumber) {
    return fiddConnector.getFiddMessageSignatureCount(messageNumber);
  }

  public byte[] getFiddMessageSignature(long messageNumber, int index) {
    return fiddConnector.getFiddMessageSignature(messageNumber, index);
  }
}

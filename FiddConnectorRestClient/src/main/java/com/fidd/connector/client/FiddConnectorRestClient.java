package com.fidd.connector.client;

import com.fidd.connector.client.api.KeysApi;
import com.fidd.connector.client.api.MessagesApi;
import com.fidd.connector.client.api.SignaturesApi;
import com.fidd.connector.client.invoker.ApiClient;
import com.fidd.connector.client.invoker.ApiException;
import com.fidd.connectors.FiddConnector;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import javax.annotation.Nullable;

public class FiddConnectorRestClient implements FiddConnector {

  private static final int HTTP_NOT_FOUND = 404;

  private final KeysApi keysApi;
  private final MessagesApi messagesApi;
  private final SignaturesApi signaturesApi;

  public FiddConnectorRestClient() {
    this("http://localhost:8080");
  }

  public FiddConnectorRestClient(String baseUrl) {
    ApiClient apiClient = new ApiClient().setBasePath(baseUrl);
    this.keysApi = new KeysApi(apiClient);
    this.messagesApi = new MessagesApi(apiClient);
    this.signaturesApi = new SignaturesApi(apiClient);
  }

  @Override
  public long getFiddMessageSize(long messageNumber) {
    return call(
        () -> messagesApi.getFiddMessageSize(messageNumber),
        "get Fidd message size: " + messageNumber);
  }

  @Override
  public List<Long> getMessageNumbersTail(int count) {
    return call(
        () -> messagesApi.getMessageNumbersTail(count), "get Fidd message numbers tail: " + count);
  }

  @Override
  public List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive) {
    return call(
        () -> messagesApi.getMessageNumbersBefore(messageNumber, count, inclusive),
        "get Fidd message numbers before: " + messageNumber);
  }

  @Override
  public List<Long> getMessageNumbersBetween(
      long latestMessage,
      boolean inclusiveLatest,
      long earliestMessage,
      boolean inclusiveEarliest,
      int count,
      boolean getLatest) {
    return call(
        () ->
            messagesApi.getMessageNumbersBetween(
                latestMessage,
                inclusiveLatest,
                earliestMessage,
                inclusiveEarliest,
                count,
                getLatest),
        "get Fidd message numbers between: " + latestMessage + " and " + earliestMessage);
  }

  @Override
  public List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprint)
      throws IOException {
    File footprintFile = writeRequestBody(footprint);
    try {
      return call(
          () -> keysApi.getFiddKeyCandidates(messageNumber, footprintFile),
          "get Fidd key candidates: " + messageNumber);
    } finally {
      deleteQuietly(footprintFile);
    }
  }

  @Override
  public @Nullable byte[] getFiddKey(long messageNumber, byte[] key) {
    File keyFile = writeRequestBodyUnchecked(key, "write Fidd key request body");
    try {
      File responseFile =
          callNullable(
              () -> keysApi.getFiddKey(messageNumber, keyFile), "get Fidd key: " + messageNumber);
      return readNullableBinaryResponse(responseFile, "read Fidd key response");
    } finally {
      deleteQuietly(keyFile);
    }
  }

  @Override
  public @Nullable byte[] getUnencryptedFiddKey(long messageNumber) {
    File responseFile =
        callNullable(
            () -> keysApi.getUnencryptedFiddKey(messageNumber),
            "get unencrypted Fidd key: " + messageNumber);
    return readNullableBinaryResponse(responseFile, "read unencrypted Fidd key response");
  }

  @Override
  public InputStream getFiddMessageChunk(long messageNumber, long offset, long length) {
    File responseFile =
        call(
            () -> messagesApi.getFiddMessageChunk(messageNumber, offset, length),
            "get Fidd message chunk: " + messageNumber);
    return openDeletingInputStream(responseFile, "open Fidd message chunk response");
  }

  @Override
  public int getFiddKeySignatureCount(long messageNumber) {
    return call(
        () -> signaturesApi.getFiddKeySignatureCount(messageNumber),
        "get Fidd key signature count: " + messageNumber);
  }

  @Override
  public byte[] getFiddKeySignature(long messageNumber, int index) {
    File responseFile =
        call(
            () -> signaturesApi.getFiddKeySignature(messageNumber, index),
            "get Fidd key signature: " + messageNumber + "/" + index);
    return readBinaryResponse(responseFile, "read Fidd key signature response");
  }

  @Override
  public int getFiddMessageSignatureCount(long messageNumber) {
    return call(
        () -> signaturesApi.getFiddMessageSignatureCount(messageNumber),
        "get Fidd message signature count: " + messageNumber);
  }

  @Override
  public byte[] getFiddMessageSignature(long messageNumber, int index) {
    File responseFile =
        call(
            () -> signaturesApi.getFiddMessageSignature(messageNumber, index),
            "get Fidd message signature: " + messageNumber + "/" + index);
    return readBinaryResponse(responseFile, "read Fidd message signature response");
  }

  private <T> T call(ApiCall<T> apiCall, String operation) {
    try {
      return apiCall.execute();
    } catch (ApiException e) {
      throw apiException(operation, e);
    }
  }

  private <T> @Nullable T callNullable(ApiCall<T> apiCall, String operation) {
    try {
      return apiCall.execute();
    } catch (ApiException e) {
      if (e.getCode() == HTTP_NOT_FOUND) {
        return null;
      }
      throw apiException(operation, e);
    }
  }

  private File writeRequestBody(byte[] body) throws IOException {
    java.nio.file.Path path = Files.createTempFile("fidd-connector-request-", ".bin");
    Files.write(path, body);
    return path.toFile();
  }

  private File writeRequestBodyUnchecked(byte[] body, String operation) {
    try {
      return writeRequestBody(body);
    } catch (IOException e) {
      throw new RuntimeException("Failed to " + operation, e);
    }
  }

  private byte[] readBinaryResponse(File file, String operation) {
    try {
      return Files.readAllBytes(file.toPath());
    } catch (IOException e) {
      throw new RuntimeException("Failed to " + operation, e);
    } finally {
      deleteQuietly(file);
    }
  }

  private @Nullable byte[] readNullableBinaryResponse(@Nullable File file, String operation) {
    if (file == null) {
      return null;
    }
    return readBinaryResponse(file, operation);
  }

  private InputStream openDeletingInputStream(File file, String operation) {
    try {
      InputStream inputStream = Files.newInputStream(file.toPath());
      return new FilterInputStream(inputStream) {
        @Override
        public void close() throws IOException {
          try {
            super.close();
          } finally {
            Files.deleteIfExists(file.toPath());
          }
        }
      };
    } catch (IOException e) {
      deleteQuietly(file);
      throw new RuntimeException("Failed to " + operation, e);
    }
  }

  private void deleteQuietly(File file) {
    try {
      Files.deleteIfExists(file.toPath());
    } catch (IOException ignored) {
    }
  }

  private RuntimeException apiException(String operation, ApiException e) {
    return new RuntimeException("Failed to " + operation + ". Response: " + e.getResponseBody(), e);
  }

  @FunctionalInterface
  private interface ApiCall<T> {
    T execute() throws ApiException;
  }
}

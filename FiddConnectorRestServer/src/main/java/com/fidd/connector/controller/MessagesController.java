package com.fidd.connector.controller;

import com.fidd.connector.service.FiddConnectorRestService;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessagesController implements MessagesApi {

  private final FiddConnectorRestService fiddConnectorRestService;

  public MessagesController(FiddConnectorRestService fiddConnectorRestService) {
    this.fiddConnectorRestService = fiddConnectorRestService;
  }

  // GET /messages/tail?count=...
  @Override
  public ResponseEntity<List<Long>> getMessageNumbersTail(Integer count) {
    List<Long> messageNumbers = fiddConnectorRestService.getMessageNumbersTail(count);
    return ResponseEntity.ok(messageNumbers);
  }

  // GET /messages/{messageNumber}/before?count=...&inclusive=...
  @Override
  public ResponseEntity<List<Long>> getMessageNumbersBefore(
      Long messageNumber, Integer count, Boolean inclusive) {
    List<Long> messageNumbers =
        fiddConnectorRestService.getMessageNumbersBefore(messageNumber, count, inclusive);
    return ResponseEntity.ok(messageNumbers);
  }

  // GET
  // /messages/range?latestMessage=...&inclusiveLatest=...&earliestMessage=...&inclusiveEarliest=...&count=...&getLatest=...
  @Override
  public ResponseEntity<List<Long>> getMessageNumbersBetween(
      Long latestMessage,
      Boolean inclusiveLatest,
      Long earliestMessage,
      Boolean inclusiveEarliest,
      Integer count,
      Boolean getLatest) {
    List<Long> messageNumbers =
        fiddConnectorRestService.getMessageNumbersBetween(
            latestMessage, inclusiveLatest, earliestMessage, inclusiveEarliest, count, getLatest);
    return ResponseEntity.ok(messageNumbers);
  }

  // GET /messages/{messageNumber}/content/size
  @Override
  public ResponseEntity<Long> getFiddMessageSize(Long messageNumber) {
    long messageSize = fiddConnectorRestService.getFiddMessageSize(messageNumber);
    return ResponseEntity.ok(messageSize);
  }

  // GET /messages/{messageNumber}/content?offset=...&length=...
  @Override
  public ResponseEntity<Resource> getFiddMessageChunk(
      Long messageNumber, Long offset, Long length) {
    InputStream chunk = fiddConnectorRestService.getFiddMessageChunk(messageNumber, offset, length);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(length)
        .body(new InputStreamResource(chunk));
  }
}

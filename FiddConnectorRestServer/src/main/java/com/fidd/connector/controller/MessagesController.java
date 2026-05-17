package com.fidd.connector.controller;

import com.fidd.connector.service.FiddConnectorRestService;
import java.io.InputStream;
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

  // GET /messages/{messageNumber}/content/size
  @Override
  public ResponseEntity<Long> getFiddMessageSize(Long messageNumber) {
    return ResponseEntity.ok(fiddConnectorRestService.getFiddMessageSize(messageNumber));
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

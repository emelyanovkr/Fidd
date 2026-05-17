package com.fidd.connector.controller;

import com.fidd.connector.service.FiddConnectorRestService;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KeysController implements KeysApi {

  private final FiddConnectorRestService fiddConnectorRestService;

  public KeysController(FiddConnectorRestService fiddConnectorRestService) {
    this.fiddConnectorRestService = fiddConnectorRestService;
  }

  // POST /messages/{messageNumber}/keys/lookup
  @Override
  public ResponseEntity<Resource> getFiddKey(Long messageNumber, Resource body) throws Exception {
    byte[] key = body.getInputStream().readAllBytes();
    return binaryResponse(fiddConnectorRestService.getFiddKey(messageNumber, key));
  }

  // POST /messages/{messageNumber}/keys/candidates
  @Override
  public ResponseEntity<List<byte[]>> getFiddKeyCandidates(Long messageNumber, Resource body)
      throws Exception {
    byte[] footprint = body.getInputStream().readAllBytes();
    return ResponseEntity.ok(
        fiddConnectorRestService.getFiddKeyCandidates(messageNumber, footprint));
  }

  // GET /messages/{messageNumber}/keys/unencrypted
  @Override
  public ResponseEntity<Resource> getUnencryptedFiddKey(Long messageNumber) {
    return binaryResponse(fiddConnectorRestService.getUnencryptedFiddKey(messageNumber));
  }

  private ResponseEntity<Resource> binaryResponse(byte[] content) {
    if (content == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(content.length)
        .body(new ByteArrayResource(content));
  }
}

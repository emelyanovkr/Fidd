package com.fidd.connector.controller;

import com.fidd.connector.service.FiddConnectorRestService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SignaturesController implements SignaturesApi {

  private final FiddConnectorRestService fiddConnectorRestService;

  public SignaturesController(FiddConnectorRestService fiddConnectorRestService) {
    this.fiddConnectorRestService = fiddConnectorRestService;
  }

  @Override
  public ResponseEntity<Resource> getFiddKeySignature(Long messageNumber, Integer index)
      throws Exception {
    return binaryResponse(fiddConnectorRestService.getFiddKeySignature(messageNumber, index));
  }

  @Override
  public ResponseEntity<Integer> getFiddKeySignatureCount(Long messageNumber) throws Exception {
    return ResponseEntity.ok(fiddConnectorRestService.getFiddKeySignatureCount(messageNumber));
  }

  @Override
  public ResponseEntity<Resource> getFiddMessageSignature(Long messageNumber, Integer index)
      throws Exception {
    return binaryResponse(fiddConnectorRestService.getFiddMessageSignature(messageNumber, index));
  }

  @Override
  public ResponseEntity<Integer> getFiddMessageSignatureCount(Long messageNumber) throws Exception {
    return ResponseEntity.ok(fiddConnectorRestService.getFiddMessageSignatureCount(messageNumber));
  }

  private ResponseEntity<Resource> binaryResponse(byte[] content) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(content.length)
        .body(new ByteArrayResource(content));
  }
}

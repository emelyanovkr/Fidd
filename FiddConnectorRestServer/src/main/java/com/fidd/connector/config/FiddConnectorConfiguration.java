package com.fidd.connector.config;

import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.folder.FolderFiddConnector;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FiddConnectorConfiguration {

  @Bean
  public FiddConnector fiddConnector(@Value("${fidd.folder-path}") Path fiddFolderPath) {
    return new FolderFiddConnector(fiddFolderPath);
  }
}

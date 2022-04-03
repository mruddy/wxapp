package com.github.mruddy.wxapp;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

public class LoggingConfig {
  public LoggingConfig() throws IOException {
    try (final InputStream inputStream = ClassLoader.getSystemResourceAsStream("logging.properties")) {
      LogManager.getLogManager().readConfiguration(inputStream);
    }
  }
}

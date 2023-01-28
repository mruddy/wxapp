package com.github.mruddy.wxapp;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.github.mruddy.wxapp.VantageVueClient.Weather;

public class App {
  static {
    if (null == System.getProperty("java.util.logging.config.file")) { // allow overriding with a logging config file external to the executable jar
      System.setProperty("java.util.logging.config.class", "com.github.mruddy.wxapp.LoggingConfig");
    }
  }
  private static final Logger log = Logger.getLogger(App.class.getCanonicalName());

  public static void main(final String[] args) throws Exception {
    final int wxStationSourcePort = Integer.parseInt(System.getenv("WXSTATION_SOURCE_PORT"), 10);
    final InetSocketAddress wxStationSource = new InetSocketAddress(System.getenv("WXSTATION_SOURCE_HOST"), wxStationSourcePort);
    final Path outputFile = Path.of(System.getenv("WXAPP_OUTPUT_FILE")).toRealPath();
    final Path outputDir = outputFile.getParent();
    App.log.info(String.format("starting: wx = %s output file = %s output dir = %s", wxStationSource, outputFile, outputDir));
    final BlockingQueue<Weather> messsageQueue = new LinkedBlockingQueue<>();
    final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleWithFixedDelay(new WxClientTask(wxStationSource, messsageQueue), 0, 1, TimeUnit.SECONDS);
    while (true) {
      try {
        final String message = messsageQueue.take().toString();
        App.log.fine(message);
        final Path tempFile = Files.createTempFile(outputDir, null, null, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r--r--")));
        Files.write(tempFile, message.getBytes(StandardCharsets.UTF_8));
        Files.move(tempFile, outputFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (final Exception e) {
        App.log.info(e.toString());
      }
    }
  }
}

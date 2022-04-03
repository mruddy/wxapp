package com.github.mruddy.wxapp;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    final Path outputPath = Path.of(System.getenv("WXAPP_OUTPUT_PATH"));
    App.log.info(String.format("starting: wx = %s output = %s", wxStationSource, outputPath));
    final BlockingQueue<Weather> messsageQueue = new LinkedBlockingQueue<>();
    final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleWithFixedDelay(new WxClientTask(wxStationSource, messsageQueue), 0, 1, TimeUnit.SECONDS);
    while (true) {
      final String message = messsageQueue.take().toString();
      Files.write(outputPath, message.getBytes(StandardCharsets.UTF_8));
      App.log.fine(message);
    }
  }
}

package com.github.mruddy.wxapp;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {
  private static final String weatherStationSourceHost = System.getenv("WXSTATION_SOURCE_HOST");
  private static final String weatherStationSourcePort = System.getenv("WXSTATION_SOURCE_PORT");
  private static final String wxappOutputPath = System.getenv("WXAPP_OUTPUT_PATH");

  public static void main(final String[] args) throws Exception {
    final int weatherStationSourcePortInt = Integer.parseInt(App.weatherStationSourcePort, 10);
    final InetSocketAddress weatherStationSource = new InetSocketAddress(App.weatherStationSourceHost, weatherStationSourcePortInt);
    final Path outputPath = Path.of(App.wxappOutputPath);
    System.out.println("wxapp starting...");
    System.out.println("source " + App.weatherStationSourceHost + " " + weatherStationSourcePortInt);
    System.out.println("output path " + App.wxappOutputPath);
    final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleWithFixedDelay(new WxClientTask(weatherStationSource, outputPath), 0, 2500, TimeUnit.MILLISECONDS);
  }
}

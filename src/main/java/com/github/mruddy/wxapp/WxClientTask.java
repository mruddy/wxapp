package com.github.mruddy.wxapp;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.github.mruddy.wxapp.VantageVueClient.Weather;

public class WxClientTask implements Runnable {
  private final InetSocketAddress server;
  private final Path path;

  public WxClientTask(final InetSocketAddress server, final Path path) {
    this.server = Objects.requireNonNull(server, "server must not be null");
    this.path = Objects.requireNonNull(path, "path must not be null");
  }

  @Override
  public void run() {
    try (final VantageVueClient vvc = new VantageVueClient(this.server)) {
      vvc.open();
      final Weather weather = vvc.getWeather();
      // 0 degrees means "no wind data"
      if ((0 < weather.getWindDirectionDegrees()) && (weather.getWindDirectionDegrees() < 361)) {
        if ((0 < weather.getWindGustDirection()) && (weather.getWindGustDirection() < 361)) {
          Files.write(this.path, weather.toString().getBytes(StandardCharsets.UTF_8));
        }
      }
    } catch (final Exception e) {
      // ConnectException Connection refused: when the weather station source is not bound to the specified endpoint
      // SocketTimeoutException Connect timed out: when a firewall between this process and the weather station host is dropping packets -- thrown from the socket.connect
      // SocketException Connection reset: when the peer resets the connection for whatever reason (sometimes associated with failing to wakeup)
      // ProtocolException: from VantageVueClient.getWeather
      // UnknownHostException: the weatherStationSourceHost is invalid -- thrown from the socket.connect
      // a possible example is an address that uses an incorrect interface specifier for a link-local address
      System.err.println(e.getClass().getName() + " " + e.getMessage());
    }
  }
}

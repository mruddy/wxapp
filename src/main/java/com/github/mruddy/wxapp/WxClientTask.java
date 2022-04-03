package com.github.mruddy.wxapp;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.mruddy.wxapp.VantageVueClient.Weather;

public class WxClientTask implements Runnable {
  private static final Logger log = Logger.getLogger(WxClientTask.class.getCanonicalName());
  private final InetSocketAddress server;
  private final BlockingQueue<Weather> messageQueue;

  public WxClientTask(final InetSocketAddress server, final BlockingQueue<Weather> messageQueue) {
    this.server = Objects.requireNonNull(server, "server must not be null");
    this.messageQueue = Objects.requireNonNull(messageQueue, "messageQueue must not be null");
  }

  @Override
  public void run() {
    try (final VantageVueClient vvc = new VantageVueClient(this.server)) {
      vvc.open();
      final int COUNT = 1000;
      vvc.request(COUNT);
      for (int i = 0; i < COUNT; i++) {
        final Weather weather = vvc.getWeather();
        if (WxClientTask.log.isLoggable(Level.FINE)) {
          WxClientTask.log.fine(weather.toString());
        }
        // 0 degrees means "no wind data"
        if ((0 < weather.getWindDirectionDegrees()) && (weather.getWindDirectionDegrees() < 361)) {
          if ((0 < weather.getWindGustDirection()) && (weather.getWindGustDirection() < 361)) {
            this.messageQueue.put(weather);
          }
        }
      }
    } catch (final Exception e) {
      // ConnectException Connection refused: when the weather station source is not bound to the specified endpoint
      // SocketTimeoutException Connect timed out: when a firewall between this process and the weather station host is dropping packets -- thrown from the socket.connect
      // SocketException Connection reset: when the peer resets the connection for whatever reason (sometimes associated with failing to wakeup)
      // ProtocolException: from VantageVueClient.getWeather
      // UnknownHostException: the weatherStationSourceHost is invalid -- thrown from the socket.connect
      // a possible example is an address that uses an incorrect interface specifier for a link-local address
      WxClientTask.log.info(e.getClass().getName() + " " + e.getMessage());
    }
  }
}

package com.github.mruddy.wxapp;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

// the specification: https://www.davisinstruments.com/support/weather/download/VantageSerialProtocolDocs_v261.pdf
// test data:
// {76, 79, 79, -20, 1, -1, 127, 77, 117, 37, 3, 53, 76, 3, 11, -1, 102, 0, 87, 0, 93, 0, 12, 0, 83, 0, -1, 127, -1, 127, 81, 0, -1, 88, -1, 99, 0, 84, 0, -1, 0, 0, 0, -1, -1, 127, 52, 1, 20, 99, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, -34, -1, 36, 117, 36, 117, 79, 117, -1, 9, 7, 14, 15, 11, 9, 22, 9, 22, 8, 8, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, 10, 13, 83, -67}
// {76, 79, 79, -20, 1, -1, 127, 76, 117, 37, 3, 53, 76, 3, 8, -1, 113, 0, 90, 0, 85, 0, 12, 0, 83, 0, -1, 127, -1, 127, 81, 0, -1, 89, -1, 100, 0, 84, 0, -1, 0, 0, 0, -1, -1, 127, 52, 1, 20, 99, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, -34, -1, 35, 117, 35, 117, 78, 117, -1, 6, 8, 15, 15, 11, 16, 22, 16, 22, 8, 8, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, -1, 127, 10, 13, 127, -30}
public class VantageVueClient implements Closeable {
  public static class Weather {
    private final String timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    private final String outsideTemperature;
    private final int windSpeedMph;
    private final int windDirectionDegrees;
    private final int windGustSpeedMph;
    private final int windGustDirection;
    private final int outsideHumidity;

    public Weather(final String outsideTemperature, final int windSpeedMph, final int windDirectionDegrees, final int windGustSpeedMph, final int windGustDirection, final int outsideHumidity) {
      this.outsideTemperature = outsideTemperature;
      this.windSpeedMph = windSpeedMph;
      this.windDirectionDegrees = windDirectionDegrees;
      this.windGustSpeedMph = windGustSpeedMph;
      this.windGustDirection = windGustDirection;
      this.outsideHumidity = outsideHumidity;
    }

    /**
     * @return the outsideHumidity outside humidity
     */
    public int getOutsideHumidity() {
      return this.outsideHumidity;
    }

    /**
     * @return the outsideTemperature outside temperature in 1/10 degree F
     */
    public String getOutsideTemperature() {
      return this.outsideTemperature;
    }

    /**
     * @return the timestamp
     */
    public String getTimestamp() {
      return this.timestamp;
    }

    /**
     * @return the windDirectionDegrees 0 means "no wind data". wind direction in degrees.
     */
    public int getWindDirectionDegrees() {
      return this.windDirectionDegrees;
    }

    /**
     * @return the windGustDirection 0 means "no wind data". 10-min wind gust direction in degrees.
     */
    public int getWindGustDirection() {
      return this.windGustDirection;
    }

    /**
     * @return the windGustSpeedMph 10-min wind gust speed in MPH
     */
    public int getWindGustSpeedMph() {
      return this.windGustSpeedMph;
    }

    /**
     * @return the windSpeedMph wind speed in MPH
     */
    public int getWindSpeedMph() {
      return this.windSpeedMph;
    }

    @Override
    public String toString() {
      final StringBuilder result = new StringBuilder(128);
      result.append("{\"t\":\"").append(this.timestamp).append("\",\"f\":\"").append(this.outsideTemperature).append("\",\"w\":").append(this.windSpeedMph).append(",\"d\":").append(this.windDirectionDegrees).append(",\"g\":").append(this.windGustSpeedMph).append(",\"gd\":").append(this.windGustDirection).append(",\"h\":").append(this.outsideHumidity).append("}");
      return result.toString();
    }
  }

  private static final int LOOP2_RESPONSE_SIZE = 99;
  private final InetSocketAddress server;
  private final Socket socket = new Socket();
  private int connectTimeoutMillis = (int) Duration.ofSeconds(5).toMillis();
  private int wakeupTimeoutMillis = (int) Duration.ofMillis(1200).toMillis();
  private int readTimeoutMillis = (int) Duration.ofSeconds(3).toMillis();
  private InputStream inputStream;
  private OutputStream outputStream;

  public VantageVueClient(final InetSocketAddress server) {
    this.server = server;
  }

  @Override
  public void close() throws IOException {
    if (this.outputStream != null) {
      this.outputStream.close();
    }
    if (this.inputStream != null) {
      this.inputStream.close();
    }
    this.socket.close();
  }

  /**
   * @return the connectTimeoutMillis
   */
  public int getConnectTimeoutMillis() {
    return this.connectTimeoutMillis;
  }

  /**
   * @return the readTimeoutMillis
   */
  public int getReadTimeoutMillis() {
    return this.readTimeoutMillis;
  }

  /**
   * @return the wakeupTimeoutMillis
   */
  public int getWakeupTimeoutMillis() {
    return this.wakeupTimeoutMillis;
  }

  public Weather getWeather() throws ProtocolException, IOException {
    this.socket.setSoTimeout(this.wakeupTimeoutMillis);
    if (!this.wakeup()) {
      throw new ProtocolException("wakeup failed");
    }
    this.socket.setSoTimeout(this.readTimeoutMillis);
    // specification page 8 and 13
    this.outputStream.write("LPS 2 1\n".getBytes(StandardCharsets.US_ASCII)); // 2 means LOOP2 packet; 1 means we request 1 packet
    this.outputStream.flush();
    // process and ACK response immediately followed by a LOOP2 packet response
    final int ack = this.inputStream.read();
    if (ack != 6) { // specification page 7
      throw new ProtocolException("ACK response not found");
    }
    final byte[] buffer = new byte[VantageVueClient.LOOP2_RESPONSE_SIZE];
    final int bytesRead = this.inputStream.read(buffer);
    // specification page 25
    if (((bytesRead != VantageVueClient.LOOP2_RESPONSE_SIZE) || (buffer[0] != 'L') || (buffer[1] != 'O') || (buffer[2] != 'O') || (buffer[4] != 1))) {
      throw new ProtocolException("LOOP2 response not found");
    }
    final ByteBuffer bufferWrapper = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
    final Weather result = new Weather(new BigDecimal(bufferWrapper.getShort(12)).movePointLeft(1).toString(), bufferWrapper.get(14), bufferWrapper.getShort(16), bufferWrapper.getShort(22), bufferWrapper.getShort(24), bufferWrapper.get(33));
    System.out.println(result);
    return result;
  }

  public void open() throws IOException {
    this.socket.setKeepAlive(false);
    System.out.println("connecting to server " + this.server);
    this.socket.connect(this.server, this.connectTimeoutMillis);
    this.inputStream = this.socket.getInputStream();
    this.outputStream = this.socket.getOutputStream();
    System.out.println("connected to server " + this.server);
  }

  /**
   * @param connectTimeoutMillis the connectTimeoutMillis to set
   */
  public void setConnectTimeoutMillis(final int connectTimeoutMillis) {
    this.connectTimeoutMillis = connectTimeoutMillis;
  }

  /**
   * @param readTimeoutMillis the readTimeoutMillis to set
   */
  public void setReadTimeoutMillis(final int readTimeoutMillis) {
    this.readTimeoutMillis = readTimeoutMillis;
  }

  /**
   * @param wakeupTimeoutMillis the wakeupTimeoutMillis to set
   */
  public void setWakeupTimeoutMillis(final int wakeupTimeoutMillis) {
    this.wakeupTimeoutMillis = wakeupTimeoutMillis;
  }

  private boolean wakeup() {
    // specification page 6 "Waking up the Console"
    for (int i = 0; i < 3; i++) { // the specification recommends 3 attempts
      try {
        this.outputStream.write('\n');
        this.outputStream.flush();
        final int newline = this.inputStream.read();
        if (newline == '\n') {
          final int carriageReturn = this.inputStream.read();
          if (carriageReturn == '\r') {
            return true;
          }
        }
      } catch (final IOException e) {
        System.out.println(e.getClass().getName() + " " + e.getMessage()); // commonly java.net.SocketTimeoutException Read timed out
      }
    }
    return false;
  }
}

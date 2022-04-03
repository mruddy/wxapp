package com.github.mruddy.wxapp;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class StdOutConsoleHandler extends StreamHandler {
  /**
   * Create a {@code StdOutConsoleHandler} for {@code System.out}.
   * <p>
   * The {@code StdOutConsoleHandler} is configured based on
   * {@code LogManager} properties (or their default values).
   *
   */
  public StdOutConsoleHandler() {
    super(System.out, new SimpleFormatter());
  }

  /**
   * Publish a {@code LogRecord}.
   * <p>
   * The logging request was made initially to a {@code Logger} object,
   * which initialized the {@code LogRecord} and forwarded it here.
   *
   * @param  record  description of the log event. A null record is
   *                 silently ignored and is not published
   */
  @Override
  public void publish(final LogRecord record) {
    super.publish(record);
    this.flush();
  }

  /**
   * Override {@code StreamHandler.close} to do a flush but not
   * to close the output stream.  That is, we do <b>not</b>
   * close {@code System.err}.
   */
  @Override
  public void close() {
    this.flush();
  }
}

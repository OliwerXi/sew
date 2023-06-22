package com.github.oliwersdk.sew.exception;

import com.github.oliwersdk.sew.scheduler.Task;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Base for internal runtime exceptions.
 */
public sealed class RuntimeExceptor extends RuntimeException
  permits UnsupportedOperation, Task.Error {
  /**
   * Instantiate a new runtime exceptor with a reason and arguments to be formatted alongside it.
   *
   * @param reason {@link String} reasoning behind this exception.
   * @param args {@link Object} array of arguments to format into the reason.
   */
  public RuntimeExceptor(String reason, Object... args) {
    super(format(requireNonNull(reason), args));
  }

  /**
   * Instantiate a new runtime exceptor with an initial cause.
   *
   * @param cause {@link Throwable} the cause behind this exception.
   */
  public RuntimeExceptor(Throwable cause) {
    super(cause);
  }

  /**
   * Instantiate a new runtime exceptor with no reason.
   */
  public RuntimeExceptor() {
    super();
  }
}
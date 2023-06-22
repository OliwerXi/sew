package com.github.oliwersdk.sew.exception;

/**
 * This exception is raised and/or used to simulate an operation
 * that is not supported in a method, constructor, and any other relative operatives.
 * @see UnsupportedOperationException
 */
public final class UnsupportedOperation extends RuntimeExceptor {
  /**
   * @see RuntimeExceptor#RuntimeExceptor(String, Object...)
   */
  public UnsupportedOperation(String reason, Object... args) {
    super(reason, args);
  }

  /**
   * @see RuntimeExceptor#RuntimeExceptor(Throwable)
   */
  public UnsupportedOperation(Throwable cause) {
    super(cause);
  }

  /**
   * @see RuntimeExceptor#RuntimeExceptor()
   */
  public UnsupportedOperation() {
    super();
  }
}
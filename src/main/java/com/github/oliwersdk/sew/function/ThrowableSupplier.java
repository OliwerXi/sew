package com.github.oliwersdk.sew.function;

/**
 * A generalized interface for handling throwable, supplying functions.
 *
 * @param <Value> type of value to be supplied.
 * @param <E> type of exception {@link ThrowableSupplier#supply()} might throw.
 */
@FunctionalInterface
public interface ThrowableSupplier<Value, E extends Exception> {
  /**
   * Invoke the (possibly) throwing code block.
   *
   * @return {@link Value} the value that was supplied.
   * @throws E if an exception is raised.
   */
  Value supply() throws E;

  /**
   * Any exceptions that may be thrown; of course those of which extend {@link Exception}.
   *
   * @param <Value> type of value to be supplied.
   * @see ThrowableSupplier
   */
  @FunctionalInterface
  interface Any<Value>
    extends ThrowableSupplier<Value, Exception> { }
}
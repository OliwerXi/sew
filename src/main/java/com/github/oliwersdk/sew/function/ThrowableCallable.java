package com.github.oliwersdk.sew.function;

import com.github.oliwersdk.sew.exception.RuntimeExceptor;

import java.util.function.Consumer;

/**
 * A callable that handles the raising of a particular exception.
 *
 * @param <E> type of exception.
 */
@FunctionalInterface
public interface ThrowableCallable<E extends Exception> {
  /**
   * Invoke the call of this callable.
   *
   * @throws E if an exception is raised.
   */
  void call() throws E;

  /**
   * @see ThrowableCallable#callOr(Consumer)
   */
  default void callOrThrow() {
    callOr(ex -> { throw new RuntimeExceptor(ex); });
  }

  /**
   * @see ThrowableCallable#callOr(Consumer)
   */
  default void callOrNothing() {
    callOr(null);
  }

  /**
   * Attempt to invoke this callable, but commit to the
   * (potentially passed) consumer if an exception is raised.
   *
   * @param otherwise {@link Consumer} potential consumer to be accepted if present and an exception was raised.
   * @see ThrowableCallable#call()
   */
  default void callOr(Consumer<Exception> otherwise) {
    try {
      call();
    } catch (Exception ex) {
      if (otherwise != null)
        otherwise.accept(ex);
    }
  }

  /**
   * The <i>"any" ({@link Exception})</i> implementation
   * of {@link ThrowableCallable}. Any child exceptions to {@link Exception} will be handled.
   */
  @FunctionalInterface
  interface Any extends ThrowableCallable<Exception> { }
}
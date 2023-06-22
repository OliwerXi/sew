package com.github.oliwersdk.sew.function;

import com.github.oliwersdk.sew.exception.RuntimeExceptor;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A consumer (in similar fashion to {@link Consumer}) that
 * handles the raising of a particular exception.
 *
 * @param <Param> type of parameter.
 * @param <E> type of exception.
 */
@FunctionalInterface
public interface ThrowableConsumer<Param, E extends Exception> {
  /**
   * Perform this operation with the parameter passed.
   *
   * @param param {@link Param} object passed through for this consumption.
   * @throws E if an exception is raised.
   */
  void consume(Param param) throws E;

  /**
   * @see ThrowableConsumer#consumeOr(Object, Consumer)
   */
  default void consumeOrThrow(Param param) {
    consumeOr(param, ex -> { throw new RuntimeExceptor(ex); });
  }

  /**
   * @see ThrowableConsumer#consumeOr(Object, Consumer)
   */
  default void consumeOrNothing(Param param) {
    consumeOr(param, null);
  }

  /**
   * Attempt to perform the consumption operation on the passed
   * parameter, but commit to the (potentially passed) consumer
   * if an exception is raised.
   *
   * @param param {@link Param} object passed through for this consumption.
   * @param otherwise {@link Consumer} potential consumer to be accepted if present and an exception was raised.
   * @see ThrowableConsumer#consume(Object)
   */
  default void consumeOr(Param param, Consumer<Exception> otherwise) {
    try {
      consume(param);
    } catch (Exception ex) {
      if (otherwise != null)
        otherwise.accept(ex);
    }
  }

  /**
   * @see ThrowableConsumer#consumeOrThrow(Object)
   */
  static <P> void consumeAnyOrThrow(P param, ThrowableConsumer.Any<P> consumer) {
    requireNonNull(consumer)
      .consumeOrThrow(param);
  }

  /**
   * The <i>"any" ({@link Exception})</i> implementation
   * of {@link ThrowableConsumer}. Any child exceptions to {@link Exception} will be handled.
   *
   * @param <Param> type of parameter.
   */
  @FunctionalInterface
  interface Any<Param> extends ThrowableConsumer<Param, Exception> { }
}
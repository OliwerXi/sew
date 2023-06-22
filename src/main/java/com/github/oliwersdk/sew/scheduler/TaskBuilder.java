package com.github.oliwersdk.sew.scheduler;

import com.github.oliwersdk.sew.function.Builder;
import com.github.oliwersdk.sew.function.ThrowableSupplier;

import java.time.Duration;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

/**
 * Class used configure and build a new task in a 'builder' fashion.
 *
 * @param <R> type of task result value.
 */
public final class TaskBuilder<R> implements Builder<Task<R>> {
  // the function used to instantiate a new task by operation and timeout
  private final BiFunction<ThrowableSupplier.Any<R>, Duration, Task<R>> instantiator;

  // the operation of which will be performed to gain the result at hand
  private ThrowableSupplier.Any<R> operation;

  // required timeframe for the task to be completed within
  private Duration timeout;

  /**
   * No-arg constructor; nothing special.
   */
  TaskBuilder(BiFunction<ThrowableSupplier.Any<R>, Duration, Task<R>> instantiator) {
    this.instantiator = instantiator;
  }

  /**
   * Assign an operation for the soon-to-be new task.
   *
   * @param operation {@link ThrowableSupplier} the operation to be assigned.
   * @return {@link TaskBuilder} current builder instance.
   */
  public TaskBuilder<R> operation(ThrowableSupplier.Any<R> operation) {
    this.operation = operation;
    return this;
  }

  /**
   * Assign a timeout to the soon-to-be new task.
   *
   * @param timeout {@link Duration} frame of the timeout to be used.
   * @return {@link TaskBuilder} current builder instance.
   */
  public TaskBuilder<R> timeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * @see Builder.P#build(Object)
   */
  @Override
  public Task<R> build() {
    // ensure the task operation's presence
    requireNonNull(operation, "task operation is missing and cannot be built");

    // instantiate a new task
    return instantiator.apply(operation, timeout);
  }
}
package com.github.oliwersdk.sew.scheduler;


import com.github.oliwersdk.sew.exception.RuntimeExceptor;
import com.github.oliwersdk.sew.function.ThrowableSupplier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.github.oliwersdk.sew.Sew.VIRTUAL_EXECUTOR;
import static com.github.oliwersdk.sew.Sew.VIRTUAL_SCHEDULED_EXECUTOR;
import static java.lang.Math.max;
import static java.time.Duration.between;
import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;

/**
 * A task used to handle a single channeled operation
 * that provide a finishing result, whether it be [non-]blocking.
 *
 * @param <R> type of result that this task will provide.
 */
public abstract class Task<R> {
  // whether the current task is completed or not
  protected final AtomicBoolean completed;

  // the operation to be performed
  protected final ThrowableSupplier.Any<R> operation;

  // timeout frame in milliseconds
  protected final long timeoutMs;

  /**
   * Assign and prepare the task's internal props respectively.
   *
   * @param operation {@link ThrowableSupplier.Any} the task's operation to be performed.
   * @param timeout {@link Duration} frame of the timeout to be used in this task.
   */
  protected Task(ThrowableSupplier.Any<R> operation, Duration timeout) {
    this.completed = new AtomicBoolean();
    this.operation = requireNonNull(operation, "task operation must not be null");
    this.timeoutMs = timeout == null ? 0L : max(timeout.toMillis(), 0);
  }

  /**
   * Complete the current task in a non-blocking manner and
   * consume the result with the passed consumer.
   *
   * @param consumer {@link Consumer} the consumer to accept on the current task result.
   */
  public abstract void complete(Consumer<Result<R>> consumer);

  /**
   * Complete the current task in a blocking manner.
   *
   * @return {@link Result}
   */
  public abstract Result<R> block();

  /**
   * Complete the current task in a blocking manner, whilst forward-throwing any raised exceptions.
   * @see Task#block()
   */
  public Result<R> blockOrThrow() {
    return block().throwIfErrorPresent();
  }

  /**
   * Block and collect the value whilst ignoring the result at hand.
   * @see Task#block()
   */
  public R blockIgnoringResult() {
    return block().value();
  }

  /**
   * Block and collect the value whilst ignoring the result at hand; but forward-throw any raised exceptions.
   * @see Task#blockOrThrow()
   */
  public R blockIgnoringResultOrThrow() {
    return blockOrThrow().value();
  }

  /**
   * Get whether the current task has completed and is done.
   *
   * @return {@link Boolean}
   */
  public final boolean done() {
    return completed.get();
  }

  /**
   * A record of data with regard to completed tasks.
   *
   * @param value {@link V} the value in which was provided upon completion.
   * @param completedIn {@link Duration} how long it took for the task to complete.
   * @param error {@link Error} potential exception upon task completion.
   * @param <V> type of value.
   */
  public record Result<V>(V value, Duration completedIn, Error error) {
    /**
     * Get whether the task was completed within the passed timeframe.
     *
     * @param timeframe {@link Duration} the timeframe in which to compare against the completion time.
     * @return {@link Boolean}
     */
    public boolean didCompleteWithin(Duration timeframe) {
      return completedIn.compareTo(requireNonNull(timeframe)) <= 0;
    }

    /**
     * Throw the respective error, should it be present.
     *
     * @return {@link Result} current result instance.
     */
    public Result<V> throwIfErrorPresent() {
      if (error != null)
        throw error;
      return this;
    }
  }

  /**
   * General exception raised and/or provided upon task failures.
   */
  public static final class Error extends RuntimeExceptor {
    /**
     * @see RuntimeExceptor#RuntimeExceptor(String, Object...)
     */
    public Error(String reason, Object... args) {
      super(reason, args);
    }

    /**
     * @see RuntimeExceptor#RuntimeExceptor(Throwable)
     */
    public Error(Throwable cause) {
      super(cause);
    }

    /**
     * @see RuntimeExceptor#RuntimeExceptor()
     */
    public Error() {
      super();
    }

    // Forwarded through completions upon tasks timing out.
    public static final Error TIMEOUT =
      new Error("task has timed out");
  }

  /**
   * A basic implementation of a {@link Task} with support for
   * blocking and non-blocking blocks.
   *
   * @param <R> type of result value.
   */
  public static final class Basic<R> extends Task<R> {
    // timeout handler
    private final AtomicReference<Timer.After> timeout;

    // prepare the internal props
    private Basic(ThrowableSupplier.Any<R> operation, Duration timeout) {
      super(operation, timeout);
      this.timeout = new AtomicReference<>(null);
    }

    /**
     * @see Task#complete(Consumer)
     */
    @Override
    public void complete(Consumer<Result<R>> consumer) {
      final var processor = VIRTUAL_EXECUTOR.submit(() -> {
        final var result = block();
        if (result != null && consumer != null)
          consumer.accept(result);

        final var existingTimeout = timeout.get();
        if (existingTimeout != null)
          existingTimeout.stop();

        timeout.set(null);
        completed.set(true);
      });

      if (timeoutMs <= 0)
        return;

      final var timer = Timer
        .after()
        .async(VIRTUAL_SCHEDULED_EXECUTOR)
        .delay(ofMillis(timeoutMs))
        .performs(() -> {
          processor.cancel(true);
          if (consumer != null)
            consumer.accept(new Result<>(null, null, Error.TIMEOUT));
          timeout.set(null);
          completed.set(true);
        });

      timer.start();
      timeout.set(timer);
    }

    /**
     * @see Task#block()
     */
    @Override
    public Result<R> block() {
      try {
        final var before = now();
        final var value = operation.supply();
        final var completionTimeframe = between(before, now());
        return new Result<>(value, completionTimeframe, null);
      } catch (InterruptedException ignored) {
        return null;
      } catch (Exception ex) {
        return new Result<>(null, null, new Error(ex));
      }
    }
  }
}
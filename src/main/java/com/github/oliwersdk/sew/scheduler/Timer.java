package com.github.oliwersdk.sew.scheduler;

import com.github.oliwersdk.sew.exception.RuntimeExceptor;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMillis;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * An abstraction layer for timers containing necessary
 * tools and mechanics for blocking and non-blocking implementations.
 *
 * @param <I> type of the respective timer implementation.
 */
public abstract sealed class Timer<I extends Timer<I>>
  permits Timer.Periodic, Timer.After {
  // the mutex used for handling on the current thread
  protected final ReentrantLock mutex;

  // ^mutex condition used for waiting on the current thread
  protected Condition mutexCondition;

  // the possibly active scheduled future for async timing
  protected final AtomicReference<ScheduledFuture<?>> activeAsync;

  // scheduled future supplier for async timing
  protected Supplier<ScheduledFuture<?>> asyncSupplier;

  // operation performed upon meeting the designated time
  protected Runnable operation;

  /**
   * Internal property initiation.
   */
  protected Timer() {
    this.mutex = new ReentrantLock();
    this.activeAsync = new AtomicReference<>(null);
  }

  /**
   * Assign an "asynchronous" execution service to use for this timer.
   *
   * @param service {@link ScheduledExecutorService} the executor to use.
   * @return {@link After} current timer instance.
   */
  public abstract I async(ScheduledExecutorService service);

  /**
   * Attempt to start the current timer.
   *
   * @return {@link Boolean} true if the timer was inactive and properly started, false otherwise.
   */
  public abstract boolean start();

  /**
   * Attempt to stop the current timer.
   *
   * @return {@link Boolean} true if the timer was active and properly stopped, false otherwise.
   */
  public boolean stop() {
    final var asyncFuture = activeAsync.get();
    if (asyncFuture != null) {
      asyncFuture.cancel(true);
      activeAsync.set(null);
      return true;
    }

    synchronized (mutex) {
      if (!mutex.isLocked())
        return false;

      if (mutexCondition != null) {
        mutexCondition.signal();
        mutexCondition = null;
      }

      mutex.unlock();
      return true;
    }
  }

  /**
   * Assign the operation to be performed by this timer when it has met its designated ticks.
   *
   * @param operation {@link Runnable} code block to be executed.
   * @return {@link I} current timer instance.
   */
  public final I performs(Runnable operation) {
    this.operation = requireNonNull(operation);
    return (I) this;
  }

  /**
   * Create a new scheduled (delayed) timer.
   *
   * @return {@link After}
   */
  public static After after() {
    return new After();
  }

  /**
   * Create a new periodic (interval) timer.
   *
   * @return {@link Periodic}
   */
  public static Periodic periodic() {
    return new Periodic();
  }

  /**
   * A timer of which performs an operation periodically with
   * any interval set to it. Blocking and non-blocking operations are supported.
   */
  public static final class Periodic extends Timer<Periodic> {
    // delay of the first timing call
    private long initialDelayMs;

    // interval of every call after first
    private long intervalMs;

    /**
     * Internal property definitions.
     */
    private Periodic() {
      super();
      this.initialDelayMs = 0;
      this.intervalMs = 0;
    }

    /**
     * @see Timer#start()
     */
    @Override
    public boolean start() {
      if (operation == null)
        return false;

      if (intervalMs <= 0)
        throw new RuntimeExceptor("interval milliseconds must be greater than 0");

      // current thread
      if (asyncSupplier == null) {
        synchronized (mutex) {
          stop(); // just in-case
          boolean first = true;

          mutexCondition = mutex.newCondition();
          while (true) {
            try {
              mutex.lockInterruptibly();
              mutexCondition.await(first ? exactInitialDelay() : intervalMs, MILLISECONDS);
              operation.run();

              if (first)
                first = false;
            } catch (InterruptedException ignored) {
              stop();
              break;
            }
          }

          stop(); // insurance...
        }
        return true;
      }

      // separate thread
      stop(); // just in-case
      activeAsync.set(asyncSupplier.get());
      return true;
    }

    /**
     * @see Timer#async(ScheduledExecutorService)
     */
    @Override
    public Periodic async(ScheduledExecutorService service) {
      requireNonNull(service);
      this.asyncSupplier = () -> service.scheduleAtFixedRate(
        operation,
        exactInitialDelay(),
        intervalMs,
        MILLISECONDS
      );
      return this;
    }

    /**
     * Assign this periodic timer with a new initial delay.
     *
     * @param duration {@link Duration} timeframe of the new initial delay.
     * @return {@link Periodic} current timer instance.
     */
    public Periodic initialDelay(Duration duration) {
      // -1 = disabled (interval is used)
      this.initialDelayMs = max(requireNonNull(duration).toMillis(), -1);
      return this;
    }

    /**
     * Assign this periodic timer with a new interval.
     *
     * @param duration {@link Duration} timeframe of the new interval.
     * @return {@link Periodic}
     */
    public Periodic interval(Duration duration) {
      this.intervalMs = max(requireNonNull(duration).toMillis(), 0);
      return this;
    }

    /**
     * Get the current initial delay.
     *
     * @return {@link Duration}
     */
    public Duration getInitialDelay() {
      return ofMillis(exactInitialDelay());
    }

    /**
     * Get the current interval.
     *
     * @return {@link Duration}
     */
    public Duration getInterval() {
      return ofMillis(this.intervalMs);
    }

    /**
     * Get the exact value of initial delay in milliseconds.
     *
     * @return {@link Long}
     */
    private long exactInitialDelay() {
      return initialDelayMs < 0 ? intervalMs : initialDelayMs;
    }
  }

  /**
   * A timer of which is scheduled to be executed after a certain duration;
   * both blocking and non-blocking operations are supported.
   */
  public static final class After extends Timer<After> {
    private long delayMs;

    private After() {
      super();
      this.delayMs = 1_000;
    }

    /** @see Timer#start() **/
    @Override
    public synchronized boolean start() {
      if (operation == null)
        return false;

      // on the current thread
      if (asyncSupplier == null) {
        synchronized (mutex) {
          stop(); // just in-case

          var now = currentTimeMillis();
          final var endsAt = now + delayMs;

          mutexCondition = mutex.newCondition();
          while (endsAt-(now = currentTimeMillis()) > 0) {
            try {
              mutex.lockInterruptibly();
              mutexCondition.await(endsAt-now, MILLISECONDS);
              operation.run();
            } catch (InterruptedException ignored) {
              break;
            } finally {
              stop();
            }
          }
        }
        return true;
      }

      // on a separate thread
      stop(); // just in-case
      activeAsync.set(asyncSupplier.get());
      return true;
    }

    /**
     * @see Timer#async(ScheduledExecutorService)
     */
    @Override
    public After async(ScheduledExecutorService service) {
      requireNonNull(service);
      this.asyncSupplier = () -> service.schedule(
        () -> { operation.run(); stop(); },
        delayMs,
        MILLISECONDS
      );
      return this;
    }

    /**
     * Set the delay of this scheduled timer.
     *
     * @param delay {@link Duration} the delay to use for this timer.
     * @return {@link After} current instance.
     */
    public After delay(Duration delay) {
      this.delayMs = max(requireNonNull(delay).toMillis(), 0);
      return this;
    }

    /**
     * Get the current delay.
     *
     * @return {@link Duration}
     */
    public Duration getDelay() {
      return ofMillis(this.delayMs);
    }
  }
}
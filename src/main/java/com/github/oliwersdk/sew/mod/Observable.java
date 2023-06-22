package com.github.oliwersdk.sew.mod;

import com.github.oliwersdk.sew.function.Cleaner;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * The representation of an internal value that is being
 * monitored (observed) for changes; e.g. being assigned another value.
 *
 * @param <Value> type of value to observe.
 */
public interface Observable<Value> extends Cleaner {
  /**
   * Add a listener for incoming updates on this observable.
   *
   * @param consumer {@link BiConsumer} the consumer to be accepted when an incoming update is queued.
   * @return {@link Observable} current observable instance.
   */
  Observable<Value> listener(BiConsumer<Value, Value> consumer);

  /**
   * Add a validation handle for incoming updates on this observable.
   *
   * @param validation {@link Validation} handle in which to test on incoming value updates.
   * @return {@link Observable} current observable instance.
   */
  Observable<Value> validation(Validation<Value> validation);

  /**
   * Update the internal value of this observable.
   *
   * @param supplier {@link Function} function to apply on the old value, to fetch the (supposedly) new value.
   * @return {@link Boolean} whether the update were successful or not by making sure it passed all validations.
   */
  boolean update(Function<Value, Value> supplier);

  /**
   * Get the internal value of this observable.
   *
   * @return {@link Value}
   */
  Value value();

  /**
   * @see Observable#update(Function)
   */
  default boolean update(Value newValue) {
    return update(ignored -> newValue);
  }

  /**
   * Create a new synchronized observable.
   *
   * @param initialValue {@link V} the initial value of the newly created observable.
   * @return {@link Observable}
   * @param <V> type of value for the new observable.
   */
  static <V> Observable<V> sync(V initialValue) {
    return new Synchronized<>(initialValue);
  }

  /**
   * This interface is used in observables to validate
   * the update of an internal value.
   *
   * @param <Value> type of value to validate.
   */
  @FunctionalInterface
  interface Validation<Value> {
    /**
     * Validate the update of an old value to a new value.
     *
     * @param oldValue {@link Value} the old value of which was set.
     * @param newValue {@link Value} the (soon <b>expected to be</b>) new value of respective observable.
     * @return {@link Boolean} whether this validation was successful and the old value can be updated with the new value.
     */
    boolean validate(Value oldValue, Value newValue);
  }

  /**
   * Standard thread-safe implementation of {@link Observable}.
   *
   * @param <Value> type of value for this observable.
   */
  final class Synchronized<Value> implements Observable<Value> {
    private final ReentrantLock mutex;
    private final Set<Validation<Value>> validations;
    private final Set<BiConsumer<Value, Value>> listeners;
    private volatile Value internal;

    private Synchronized(Value initialValue) {
      this.mutex = new ReentrantLock();
      this.validations = new LinkedHashSet<>();
      this.listeners = new LinkedHashSet<>();
      this.internal = initialValue;
    }

    @Override
    public Observable<Value> listener(BiConsumer<Value, Value> consumer) {
      requireNonNull(consumer, "listener consumer must not be null");
      return doSynchronized(ignored -> {
        listeners.add(consumer);
        return this;
      });
    }

    @Override
    public Observable<Value> validation(Validation<Value> validation) {
      requireNonNull(validation, "validation must not be null");
      return doSynchronized(ignored -> {
        validations.add(validation);
        return this;
      });
    }

    @Override
    public boolean update(Function<Value, Value> supplier) {
      requireNonNull(supplier, "new-value supplier must not be null");
      return doSynchronized(oldValue -> {
        final var newValue = supplier.apply(oldValue);
        boolean validated = true;

        for (final var validation : validations) {
          if (validation.validate(oldValue, newValue))
            continue;

          validated = false;
          break;
        }

        if (!validated)
          return false;

        internal = newValue;
        for (final var listener : listeners)
          listener.accept(oldValue, newValue);
        return true;
      });
    }

    @Override
    public Value value() {
      return doSynchronized(val -> val);
    }

    @Override
    public void cleanResources() {
      validations.clear();
      listeners.clear();
      internal = null;
    }

    private <T> T doSynchronized(Function<Value, T> func) {
      requireNonNull(func);
      synchronized (mutex) {
        mutex.lock();
        final var result = func.apply(this.internal);
        mutex.unlock();
        return result;
      }
    }
  }
}
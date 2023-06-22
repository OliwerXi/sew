package com.github.oliwersdk.sew.scheduler;

import com.github.oliwersdk.sew.exception.RuntimeExceptor;
import com.github.oliwersdk.sew.function.Cleaner;
import com.github.oliwersdk.sew.function.ThrowableSupplier;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.IdentityHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A base for scheduling mechanics such as tasks (and potentially timers in the foreseeable future).
 */
public interface Scheduler extends Cleaner {
  /**
   * Register a task implementation by its respective class.
   *
   * @param taskImpl {@link Class} class of the task implementation to register.
   * @return {@link Scheduler} current scheduler instance.
   */
  <T extends Task<?>> Scheduler registerTaskImpl(Class<T> taskImpl);

  /**
   * Deregister a task implementation by its respective class.
   *
   * @param taskImpl {@link Class} class of the task implementation to deregister.
   * @return {@link Scheduler} current scheduler instance.
   */
  <T extends Task<?>> Scheduler deregisterTaskImpl(Class<T> taskImpl);

  /**
   * Create a new task builder with {@link R} as the result.
   *
   * @param taskImpl {@link Class} class of the task implementation to use.
   * @return {@link TaskBuilder} respective task builder, but null if the task implementation class was not registered.
   */
  <T extends Task<?>, R> TaskBuilder<R> taskBuilder(Class<T> taskImpl);

  /**
   * @param ignored {@link Class} class of the task result type; merely to infer the generic
   *                             parameter without the use of explicit defining.
   * @see Scheduler#taskBuilder(Class)
   */
  default <T extends Task<?>, R> TaskBuilder<R> taskBuilder(Class<R> ignored, Class<T> taskImpl) {
    return taskBuilder(taskImpl);
  }

  /**
   * Instantiate a new instance of the standard {@link Scheduler} implementation.
   *
   * @return {@link Scheduler}
   */
  static Scheduler standard() {
    return new Standard();
  }

  /**
   * The basic, standard implementation of a scheduler.
   */
  final class Standard implements Scheduler {
    // Cached constructors of registered task classes.
    private final Map<Class<? extends Task<?>>, Constructor<? extends Task<?>>> taskImpl;

    /**
     * Instantiate a new basic, standard implementation of a scheduler.
     */
    private Standard() {
      this.taskImpl = new IdentityHashMap<>();
      registerTaskImpl(Task.Basic.class);
    }

    /**
     * @see Scheduler#registerTaskImpl(Class)
     */
    @Override
    public <T extends Task<?>> Scheduler registerTaskImpl(Class<T> taskImpl) {
      constructTask(taskImpl, false);
      return this;
    }

    /**
     * @see Scheduler#deregisterTaskImpl(Class)
     */
    @Override
    public <T extends Task<?>> Scheduler deregisterTaskImpl(Class<T> taskImpl) {
      this.taskImpl.remove(checkImpl(taskImpl));
      return this;
    }

    /**
     * @see Scheduler#taskBuilder(Class)
     */
    @Override
    public <T extends Task<?>, R> TaskBuilder<R> taskBuilder(Class<T> taskImpl) {
      final var constructor = constructTask(taskImpl, true);
      requireNonNull(constructor, "could not create constructor of task implementation");

      return new TaskBuilder<>((operation, timeout) -> {
        try {
          return (Task<R>) constructor.newInstance(operation, timeout);
        } catch (Exception ex) {
          throw new RuntimeExceptor(ex);
        }
      });
    }

    /**
     * @see Scheduler#cleanResources()
     */
    @Override
    public void cleanResources() {
      // clear all task implementations
      taskImpl.clear();
    }

    /**
     * Fetch existing constructor of a task if the containment check is true,
     * otherwise attempt the creation of a new constructor.
     *
     * @param of {@link Class} class of task implementation to either fetch or construct a new instantiation 'protocol' for.
     * @param checkContainment {@link Boolean} whether to check for an existing constructor in the cache.
     * @return {@link Constructor} respective constructor from internal cache or freshly fetched.
     */
    private Constructor<? extends Task<?>> constructTask(Class<? extends Task<?>> of, boolean checkContainment) {
      checkImpl(of);
      if (checkContainment) {
        final var existing = taskImpl.get(of);
        if (existing != null)
          return existing;
      }

      try {
        final var constructor = of.getDeclaredConstructor(
          ThrowableSupplier.Any.class,
          Duration.class
        );
        constructor.setAccessible(true);
        taskImpl.put(of, constructor);
        return constructor;
      } catch (Exception ex) {
        throw new RuntimeExceptor(ex);
      }
    }

    /**
     * Ensure that the task implementation class argument passed is not null.
     *
     * @param of {@link Class} task implementation to check nullability on.
     * @return {@link Class} the class that was passed.
     */
    private static Class<? extends Task<?>> checkImpl(Class<? extends Task<?>> of) {
      if (of == null)
        throw new RuntimeExceptor("task implementation must not be null");
      return of;
    }
  }
}
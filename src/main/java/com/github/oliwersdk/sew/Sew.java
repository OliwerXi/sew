package com.github.oliwersdk.sew;

import com.github.oliwersdk.sew.exception.UnsupportedOperation;
import com.github.oliwersdk.sew.function.Cleaner;
import com.github.oliwersdk.sew.function.ThrowableCallable;
import com.github.oliwersdk.sew.scheduler.Scheduler;
import com.github.oliwersdk.sew.scheduler.Task;
import com.github.oliwersdk.sew.scheduler.TaskBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static com.github.oliwersdk.sew.Reflect.fieldAccessor;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;

/**
 * This class holds and handles the most utilities in the entire SEW module.
 * Utility functions that are not backed by any storage are not included.
 */
public final class Sew implements Cleaner {
  // The one and only instance of this class needed.
  // Used to easily access inner functionality via statically declared methods
  // to avoid excessive calls and references to this field.
  private static final Sew ME = new Sew();

  // Shared virtual-thread executor service.
  public static final ExecutorService VIRTUAL_EXECUTOR =
    newVirtualThreadPerTaskExecutor();

  // Shared virtual-thread scheduled executor service (sharing thread factory with VIRTUAL_EXECUTOR).
  public static final ScheduledExecutorService VIRTUAL_SCHEDULED_EXECUTOR =
    newSingleThreadScheduledExecutor(
      fieldAccessor("java.util.concurrent.ThreadPerTaskExecutor")
        .select("factory")
        .value(VIRTUAL_EXECUTOR)
    );

  // This object is a shared representation of a "non-existent value."
  public static final Object NONE = new Object();

  // The internally set (standard) scheduler.
  private final Scheduler scheduler;

  /**
   * No need for outer instantiations of this class so the constructor will
   * raise an {@link UnsupportedOperation} exception if any unknown caller
   * decides to instantiate using reflection.
   */
  private Sew() {
    final var caller = StackWalker
      .getInstance(RETAIN_CLASS_REFERENCE)
      .getCallerClass();

    if (caller != Sew.class)
      throw new UnsupportedOperation("must not instantiate singleton \"%s\"", getClass().getName());

    // internal props
    this.scheduler = Scheduler.standard();
  }

  /**
   * Execute a runnable inside a virtual thread using the shared {@link Sew#VIRTUAL_EXECUTOR}.
   *
   * @param runnable {@link Runnable} the runnable to be executed.
   */
  public static void execVirtually(Runnable runnable) {
    requireNonNull(runnable);
    VIRTUAL_EXECUTOR.execute(runnable);
  }

  /**
   * Perform a task operation in a [non-]blocking manner.
   *
   * @param taskImpl {@link Class} class of the type of task implementation to use.
   * @param async {@link Boolean} whether or not to perform a non-blocking operation.
   * @param operation {@link ThrowableCallable.Any} the operation to be performed.
   * @param <T> type of task implementation.
   */
  public static <T extends Task<?>> void doTask(Class<T> taskImpl,
                                                boolean async,
                                                ThrowableCallable.Any operation) {
    requireNonNull(operation);
    final var task = taskBuilder(taskImpl)
      .operation(() -> {
        operation.call();
        return null;
      })
      .build();

    if (async)
      task.complete(null);
    else
      task.blockOrThrow();
  }

  /**
   * Perform a task operation in a [non-]blocking manner using the basic implementation.
   *
   * @see Sew#doTask(Class, boolean, ThrowableCallable.Any)
   */
  public static void doTask(boolean async, ThrowableCallable.Any operation) {
    doTask(Task.Basic.class, async, operation);
  }

  /**
   * @see Scheduler#taskBuilder(Class)
   */
  public static <T extends Task<?>, R> TaskBuilder<R> taskBuilder(Class<T> taskImpl) {
    return ME.scheduler.taskBuilder(taskImpl);
  }

  /**
   * @see Scheduler#taskBuilder(Class, Class)
   */
  public static <T extends Task<?>, R> TaskBuilder<R> taskBuilder(Class<R> resultClass, Class<T> taskImpl) {
    return ME.scheduler.taskBuilder(resultClass, taskImpl);
  }

  /**
   * @see Scheduler#registerTaskImpl(Class)
   */
  public static <T extends Task<?>> void registerTaskImpl(Class<T> taskImpl) {
    ME.scheduler.registerTaskImpl(taskImpl);
  }

  /**
   * @see Scheduler#deregisterTaskImpl(Class)
   */
  public static <T extends Task<?>> void deregisterTaskImpl(Class<T> taskImpl) {
    ME.scheduler.deregisterTaskImpl(taskImpl);
  }

  /**
   * Clean up all the resources from SEW modules.
   */
  @Override
  public void cleanResources() {
    scheduler.cleanResources();
  }

  /**
   * @see Sew#cleanResources()
   */
  public static void cleanup() {
    ME.cleanResources();
  }
}
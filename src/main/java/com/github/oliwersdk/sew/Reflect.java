package com.github.oliwersdk.sew;

import com.github.oliwersdk.sew.exception.UnsupportedOperation;
import com.github.oliwersdk.sew.function.ValueMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

import static com.github.oliwersdk.sew.function.ThrowableConsumer.consumeAnyOrThrow;
import static com.github.oliwersdk.sew.function.ValueMapper.*;
import static java.util.Objects.requireNonNull;

/**
 * Utility class consistent of helper functionality related to reflection.
 */
public final class Reflect {
  // No instantiation.
  private Reflect() {
    throw new UnsupportedOperation();
  }

  /**
   * Get a class by its name.
   *
   * @param name {@link String} the name of the class to fetch.
   * @return {@link Class}
   */
  public static Class<?> classOf(String name) {
    return fromOrThrow(name, Class::forName);
  }

  /**
   * Create a new constructor accessor with parent argument passed along.
   *
   * @param parent {@link Class} class of the parent to select, process and instantiate constructors from.
   * @return {@link ConstructAccessor}
   */
  public static ConstructAccessor constructAccessor(Class<?> parent) {
    return new ConstructAccessor(parent);
  }

  /**
   * @see Reflect#constructAccessor(Class)
   */
  public static ConstructAccessor constructAccessor(String parentClassName) {
    return constructAccessor(classOf(parentClassName));
  }

  /**
   * Create a new field accessor with parent argument passed along.
   *
   * @param parent {@link Class} class of the parent to select and process fields from.
   * @return {@link FieldAccessor}
   */
  public static FieldAccessor fieldAccessor(Class<?> parent) {
    return new FieldAccessor(parent);
  }

  /**
   * @see Reflect#fieldAccessor(Class)
   */
  public static FieldAccessor fieldAccessor(String parentClassName) {
    return fieldAccessor(classOf(parentClassName));
  }

  /**
   * Create a new method accessor with parent argument passed along.
   *
   * @param parent {@link Class} class of the parent to select and process methods from.
   * @return {@link MethodAccessor}
   */
  public static MethodAccessor methodAccessor(Class<?> parent) {
    return new MethodAccessor(parent);
  }

  /**
   * @see Reflect#methodAccessor(Class)
   */
  public static MethodAccessor methodAccessor(String parentClassName) {
    return methodAccessor(classOf(parentClassName));
  }

  /**
   * Class used to access, process and instantiate different constructors from a single parent class.
   */
  public static final class ConstructAccessor
    extends Accessor<Constructor<?>, Class<?>[], Object, ConstructAccessor> {
    /**
     * Instantiate a new constructor accessor with the passed parent as its anchor.
     *
     * @param parent {@link Class} the parent to be said anchor.
     */
    private ConstructAccessor(Class<?> parent) {
      super(parent);
    }

    /**
     * @see Accessor#parent(Class)
     */
    @Override
    public ConstructAccessor parent(Class<?> newParent) {
      return new ConstructAccessor(newParent);
    }

    /**
     * Select a constructor from the current parent by its parameter types.
     *
     * @param parameterTypes {@link Class} array of class references to respective
     *                                    parameter types of the constructor wished upon.
     * @return {@link ConstructAccessor} current accessor instance.
     */
    @Override
    public ConstructAccessor select(Class<?>[] parameterTypes, Object ignored) {
      return selectAccessible($ -> parent.getDeclaredConstructor(parameterTypes));
    }

    /**
     * @see Accessor#annotation(Class)
     */
    @Override
    public <T extends Annotation> T annotation(Class<T> annotationClass) {
      return selected().getAnnotation(annotationClass);
    }

    /**
     * Instantiate a new object from the currently selected constructor.
     *
     * @param arguments {@link Object} array of arguments to pass along to the constructor.
     * @return {@link T} fresh instantiated object.
     * @param <T> type of object.
     */
    public <T> T construct(Object... arguments) {
      return (T) fromOrThrow(selected(), c -> c.newInstance(arguments));
    }

    /**
     * @see ConstructAccessor#construct(Object...)
     */
    public <T> T construct(Class<T> ignored, Object... arguments) {
      return construct(arguments);
    }

    /**
     * Instantiate a new object from the currently selected constructor.
     * <b>null</b> will be returned if there was an issue.
     *
     * @param arguments {@link Object} array of arguments to pass along to the constructor.
     * @return {@link T} fresh instantiated object if successful, null otherwise.
     * @param <T> type of object.
     */
    public <T> T constructOrNull(Object... arguments) {
      return (T) solidOrNull(ignored -> selected().newInstance(arguments));
    }

    /**
     * @see ConstructAccessor#constructOrNull(Object...)
     */
    public <T> T constructOrNull(Class<T> ignored, Object... arguments) {
      return constructOrNull(arguments);
    }
  }

  /**
   * Class used to access and handle different fields from a single parent class.
   */
  public static final class FieldAccessor
    extends Accessor<Field, String, Object, FieldAccessor> {
    /**
     * Instantiate a new field accessor with the passed parent as its anchor.
     *
     * @param parent {@link Class} the parent to be said anchor.
     */
    private FieldAccessor(Class<?> parent) {
      super(parent);
    }

    /**
     * @see Accessor#parent(Class)
     */
    @Override
    public FieldAccessor parent(Class<?> newParent) {
      return new FieldAccessor(newParent);
    }

    /**
     * Select a field by its name.
     *
     * @param fieldName {@link String} name of the field to be selected.
     * @return {@link FieldAccessor} current accessor instance.
     * @see Accessor#select(Object, Object)
     */
    @Override
    public FieldAccessor select(String fieldName, Object ignored) {
      return selectAccessible($ -> parent.getDeclaredField(fieldName));
    }

    /**
     * Get the value set in the selected field of the passed object instance.
     *
     * @param ref {@link Object} instance of the object to fetch the field value from.
     * @return {@link T} value from the selected field of the respective instance.
     * @param <T> type of return value.
     */
    public <T> T value(Object ref) {
      return (T) fromOrThrow(ref, selected()::get);
    }

    /**
     * Set the value of the selected field in a particular object instance.
     *
     * @param ref {@link Object} instance of the object to set the value in.
     * @param to {@link Object} instance to set the field's new value to.
     * @return {@link FieldAccessor} current accessor instance.
     */
    public FieldAccessor setValue(Object ref, Object to) {
      requireNonNull(ref, "object instance to set field value of must not be null");
      consumeAnyOrThrow(selected(), field -> {
        // the field must not be final
        if (hasModifier(Modifier.FINAL))
          return;

        // attempt to update the value
        field.set(ref, to);
      });
      return this;
    }

    /**
     * Get an annotation placed on the selected field, if any.
     * @see Accessor#annotation(Class)
     */
    @Override
    public <T extends Annotation> T annotation(Class<T> annotationClass) {
      return selected().getAnnotation(annotationClass);
    }

    /**
     * @see Accessor#modifiers()
     */
    @Override
    public int modifiers() {
      return selected().getModifiers(); // field is not an 'Executable' so this will do
    }
  }

  /**
   * Class used to access and handle different methods from a single parent class.
   */
  public static final class MethodAccessor
    extends Accessor<Method, String, Class<?>[], MethodAccessor> {
    /**
     * Instantiate a new method accessor with the passed parent as its anchor.
     *
     * @param parent {@link Class} the parent to be said anchor.
     */
    private MethodAccessor(Class<?> parent) {
      super(parent);
    }

    /**
     * @see Accessor#parent(Class)
     */
    @Override
    public MethodAccessor parent(Class<?> newParent) {
      return new MethodAccessor(newParent);
    }

    /**
     * Select a method by its declaration name and potential argument types.
     *
     * @param methodName {@link String} name of the method to be selected.
     * @param params {@link Class} array of parameter types.
     * @return {@link MethodAccessor} current accessor instance.
     * @see Accessor#select(Object, Object)
     */
    @Override
    public MethodAccessor select(String methodName, Class<?>[] params) {
      return selectAccessible($ -> parent.getDeclaredMethod(methodName, params));
    }

    /**
     * Call the selected method on the passed instance with respective arguments.
     *
     * @param ref {@link Object} (null = static) instance of the object to invoke the selected method on.
     * @param arguments {@link Object} array of possible arguments to pass along during invocation.
     * @return {@link T} potential value from a successful method invocation, otherwise {@link Sew#NONE}.
     * @param <T> type of returning value.
     */
    public <T> T invoke(Object ref, Object... arguments) {
      return (T) solidOrThrow($ -> {
        final var selector = selected();
        final var result = selector.invoke(ref, arguments);
        return selector.getReturnType() == Void.TYPE ? Sew.NONE : result;
      });
    }

    /**
     * Get an annotation placed on the selected method, if any.
     * @see Accessor#annotation(Class)
     */
    @Override
    public <T extends Annotation> T annotation(Class<T> annotationClass) {
      return selected().getAnnotation(annotationClass);
    }
  }

  /**
   * Base layer for internal accessors in {@link Reflect}.
   * Permitting {@link FieldAccessor} and {@link MethodAccessor}.
   *
   * @param <S> type of selectable.
   * @param <I> type of the identifier used to identify a selectable via.
   * @param <A> type of the respective implementation.
   */
  public static abstract sealed class Accessor<S, I, E, A extends Accessor<S, I, E, A>>
    permits ConstructAccessor, FieldAccessor, MethodAccessor {
    // the parent class to select fields from
    protected final Class<?> parent;

    // currently selected field
    protected S selected;

    /**
     * Handle internal properties of the respective accessor.
     *
     * @param parent {@link Class} the parent to be the anchor to this accessor.
     */
    protected Accessor(Class<?> parent) {
      this.parent = requireNonNull(parent, "parent class of field accessibility must not be null");
    }

    /**
     * Switch to another parent by its class specifier.
     *
     * @param newParent {@link Class} class of the new parent to switch to.
     * @return {@link A} new accessor instance created with the new, respective parent class.
     */
    public abstract A parent(Class<?> newParent);

    /**
     * Select a particular {@link S} from the parent anchor of this accessor.
     *
     * @param identifier {@link I} (possibly)-unique identifier of the selectable to be selected.
     * @param data {@link E} potential extra data, if needed.
     * @return {@link A} current accessor instance.
     */
    public abstract A select(I identifier, E data);

    /**
     * @see Accessor#select(Object, Object)
     */
    public A select(I identifier) {
      return select(identifier, null);
    }

    /**
     * Get an annotation from the relative selector in this accessor if supported.
     *
     * @param annotationClass {@link Class} class of the annotation to fetch data from.
     * @return {@link T} instance of the annotation that was fetched.
     * @param <T> type of annotation.
     */
    public <T extends Annotation> T annotation(Class<T> annotationClass) {
      throw new UnsupportedOperation("%s#annotation(Class)", getClass().getSimpleName());
    }

    /**
     * Get the modifiers of the current selectable, if possible, otherwise raise an exception.
     *
     * @return {@link Integer}
     */
    public int modifiers() {
      final var s = selected();
      if (s instanceof Executable exec)
        return exec.getModifiers();
      throw new UnsupportedOperation();
    }

    /**
     * Get whether the current selectable has a specific modifier.
     *
     * @param modifier {@link Integer} the modifier to look for.
     * @return {@link Boolean}
     */
    public boolean hasModifier(int modifier) {
      return (modifiers() & modifier) != 0;
    }

    /**
     * Get the internal selectable value of this accessor.
     *
     * @return {@link S}
     */
    public S internal() {
      return this.selected;
    }

    /**
     * <i>Ensure the presence of and get</i> the set selectable.
     *
     * @return {@link S} respective selectable instance.
     */
    protected final S selected() {
      if (selected == null)
        throw new NullPointerException("no selectable set");
      return this.selected;
    }

    /**
     * Set a selectable and assign its potential level of accessibility to true.
     *
     * @param mapper {@link ValueMapper} the mapper used to map from parent to expected selectable.
     * @return {@link A} current accessor instance.
     */
    protected final A selectAccessible(ValueMapper<Class<?>, S> mapper) {
      final var mapped = fromOrThrow(this.parent, mapper);
      if (mapped instanceof AccessibleObject accessible)
        accessible.setAccessible(true);

      this.selected = mapped;
      return (A) this;
    }
  }
}
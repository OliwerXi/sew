package com.github.oliwersdk.sew.function;

import com.github.oliwersdk.sew.exception.RuntimeExceptor;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * This interface represents the base of any and all - and/or most - builders.
 *
 * @param <Value> type of value this builder will result in.
 */
@FunctionalInterface
public interface Builder<Value> {
  /**
   * Build the <i>(most likely configured)</i> instance.
   *
   * @return {@link Value}
   */
  Value build() throws Exception;

  /**
   * Perform the build on this builder and raise the exception
   * that was provided if a default value was requested.
   *
   * @return {@link Value} built value.
   * @see Builder#buildOrDefault(Function)
   */
  default Value buildOrThrow() {
    return buildOrDefault(ex -> { throw new RuntimeExceptor(ex); });
  }

  /**
   * Perform the build on this builder, but return null if a default value was requested.
   *
   * @return {@link Value} built value, or null if failed.
   * @see Builder#buildOrDefault(Function)
   */
  default Value buildOrNull() {
    return buildOrDefault(ignored -> null);
  }

  /**
   * Perform the build on this builder and if it fails due to a raised exception,
   * invoke the passed function to replace as a default value supplier.
   *
   * @param def {@link Function} function used to supply a default value.
   * @return {@link Value} built value at hand, or the collected default value from the supplying function.
   */
  default Value buildOrDefault(Function<Exception, Value> def) {
    try {
      return build();
    } catch (Exception ex) {
      return requireNonNull(def)
        .apply(ex);
    }
  }

  @FunctionalInterface
  interface P<Value, Type> extends Builder<Value> {
    /**
     * Build the <i>(most likely configured)</i> instance
     * with the passed parameterized data.
     *
     * @param param {@link Type} extra data parameterized for this building process.
     * @return {@link Value}
     */
    Value build(Type param) throws Exception;

    /**
     * @see P#buildOrDefault(Object, Function)
     */
    default Value buildOrThrow(Type param) {
      return buildOrDefault(param, ex -> { throw new RuntimeExceptor(ex); });
    }

    /**
     * @see P#buildOrDefault(Object, Function)
     */
    default Value buildOrNull(Type param) {
      return buildOrDefault(param, ignored -> null);
    }

    /**
     * The same process as {@link Builder#buildOrDefault(Function)},
     * only this one takes a typed parameter for extra data to be processed.
     */
    default Value buildOrDefault(Type param, Function<Exception, Value> def) {
      try {
        return build(param);
      } catch (Exception ex) {
        return requireNonNull(def)
          .apply(ex);
      }
    }

    /**
     * Pass along null to the actual building method
     * as the default non-param representation.
     *
     * @see P#build(Object)
     */
    @Override
    default Value build() throws Exception {
      return build(null);
    }
  }
}
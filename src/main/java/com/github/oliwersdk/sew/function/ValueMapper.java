package com.github.oliwersdk.sew.function;


import com.github.oliwersdk.sew.exception.RuntimeExceptor;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * An interface which handles mapping and conversions from and to values.
 *
 * @param <From> type to map from.
 * @param <To> type to map to.
 */
@FunctionalInterface
public interface ValueMapper<From, To> {
  /**
   * Map the passed object to its corresponding {@link To} value.
   *
   * @param from {@link From} the object to map from.
   * @return {@link To} freshly mapped instance.
   * @throws Exception if a failure occurred during mapping.
   */
  To map(From from) throws Exception;

  /**
   * Map the passed object to its corresponding {@link To} value and should it fail;
   * use the function provided to generate and/or get a default value.
   *
   * @see ValueMapper#map(Object)
   */
  default To mapOrDefault(From from, Function<Exception, To> defaultSupplier) {
    try {
      return map(from);
    } catch (Exception ex) {
      return requireNonNull(defaultSupplier, "default-value supplier must not be null").apply(ex);
    }
  }

  /**
   * Map the passed object to its corresponding {@link To} value and should it fail; return null.
   *
   * @see ValueMapper#mapOrDefault(Object, Function)
   */
  default To mapOrNull(From from) {
    return mapOrDefault(from, ignored -> null);
  }

  /**
   * Map the passed object to its corresponding {@link To} and wrap the potential value in an {@link Optional}.
   *
   * @see ValueMapper#mapOrNull(Object)
   */
  default Optional<To> mapOptional(From from) {
    return ofNullable(mapOrNull(from));
  }

  /**
   * Generate an object mapper that returns the argument that was provided.
   *
   * @return {@link ValueMapper}
   * @param <Type> type of item.
   */
  static <Type> ValueMapper<Type, Type> same() {
    return v -> v;
  }

  /**
   * @see ValueMapper#mapOrDefault(Object, Function)
   */
  static <From, To> To fromOrThrow(From from, ValueMapper<From, To> mapper) {
    return requireNonNull(mapper)
      .mapOrDefault(from, ex -> { throw new RuntimeExceptor(ex); });
  }

  /**
   * @see ValueMapper#mapOrNull(Object)
   */
  static <From, To> To fromOrNull(From from, ValueMapper<From, To> mapper) {
    return requireNonNull(mapper).mapOrNull(from);
  }

  /**
   * @see ValueMapper#mapOptional(Object)
   */
  static <From, To> Optional<To> fromOptional(From from, ValueMapper<From, To> mapper) {
    return requireNonNull(mapper).mapOptional(from);
  }

  /**
   * Map null into a result solidly, that said; ignore the 'from' parameter.
   * Throw the <b>potential</b> exception that was raised internally.
   *
   * @see ValueMapper#fromOrThrow(Object, ValueMapper)
   */
  static <To> To solidOrThrow(ValueMapper<Object, To> mapper) {
    return fromOrThrow(null, mapper);
  }

  /**
   * Map null into a result solidly, that said; ignore the 'from' parameter.
   * If an exception was raised internally, the returned value will be null.
   *
   * @see ValueMapper#fromOrNull(Object, ValueMapper)
   */
  static <To> To solidOrNull(ValueMapper<Object, To> mapper) {
    return fromOrNull(null, mapper);
  }
}
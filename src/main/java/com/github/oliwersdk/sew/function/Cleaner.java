package com.github.oliwersdk.sew.function;

/**
 * Any and all children of this interface are types that are have a baseline of
 * resources with the wish of being "cleaned up" when their use have passed.
 */
@FunctionalInterface
public interface Cleaner {
  /**
   * Clean up the resources of this cleaner respectively.
   */
  void cleanResources();
}
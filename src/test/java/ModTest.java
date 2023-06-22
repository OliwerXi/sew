import com.github.oliwersdk.sew.mod.Observable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
public class ModTest {
  @Test
  void observers() {
    final var observer = Observable
      .sync("hello")
      .validation((ignored, newValue) -> newValue.equals("hello world"))
      .listener((oldValue, newValue) -> System.out.printf("observable changed from '%s' to '%s'%n", oldValue, newValue));

    assertFalse(observer.update("world hello"));
    assertTrue(observer.update("hello world"));
    observer.cleanResources();
  }
}
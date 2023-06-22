import com.github.oliwersdk.sew.Reflect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
public class ReflectTest {
  private static final byte EXEMPLAR = 0x01;

  @Test
  void fieldAccessor() {
    assertEquals(
      (byte) Reflect
        .fieldAccessor(ReflectTest.class)
        .select("EXEMPLAR")
        .value(null),
      EXEMPLAR
    );
  }

  @Test
  void methodAccessor() {
    assertEquals(
      Reflect
        .methodAccessor(ReflectTest.class)
        .select("echo", new Class[] { String.class })
        .invoke(null, "hello"), // null (no ref) = static method
      "hello"
    );
  }

  private static String echo(String text) {
    return text;
  }
}
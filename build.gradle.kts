plugins {
  id("java")
}

group = "com.github.oliwersdk"
version = "0.0.1"

val exports = arrayOf(
  "java.base/java.util.concurrent=ALL-UNNAMED"
)

repositories {
  mavenCentral()
}

dependencies {
  // unit testing
  testImplementation(platform("org.junit:junit-bom:5.9.1"))
  testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<JavaCompile> {
  options.compilerArgs.addAll(exports())
}

tasks.test {
  useJUnitPlatform()
  jvmArgs(*exports("opens"))
}

fun exports(type: String = "exports"): Array<String> {
  return arrayOf(
    "--enable-preview",
    *exports
      .map { ex -> "--add-$type=$ex" }
      .toTypedArray()
  )
}
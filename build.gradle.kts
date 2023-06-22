plugins {
  java
  `maven-publish`
}

val exports = arrayOf("java.base/java.util.concurrent=ALL-UNNAMED")

group = "com.github.oliwersdk"
version = "0.0.1"

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

publishing {
  publications {
    create<MavenPublication>("library") {
      artifactId = "sew"
      from(components["java"])
    }
  }
}

fun exports(type: String = "exports"): Array<String> {
  return arrayOf(
    "--enable-preview",
    *exports
      .map { ex -> "--add-$type=$ex" }
      .toTypedArray()
  )
}
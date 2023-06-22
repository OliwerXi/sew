plugins {
  java
  `maven-publish`
}

val pkg = "com.github.oliwersdk"
val ver = "0.0.1"
val exports = arrayOf("java.base/java.util.concurrent=ALL-UNNAMED")

group = pkg
version = ver

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
    create<MavenPublication>("maven") {
      groupId = pkg
      artifactId = "sew"
      version = ver
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
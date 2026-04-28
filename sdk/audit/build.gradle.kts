plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("otel.animalsniffer-conventions")
}

description = "OpenTelemetry Audit Logging SDK"
otelJava.moduleName.set("io.opentelemetry.sdk.audit")

dependencies {
  api(project(":api:audit"))
  api(project(":sdk:common"))

  annotationProcessor("com.google.auto.value:auto-value")

  testImplementation("org.awaitility:awaitility")
  testImplementation("com.google.guava:guava")
}

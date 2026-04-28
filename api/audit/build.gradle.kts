plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("otel.animalsniffer-conventions")
}

description = "OpenTelemetry Audit Logging API"
otelJava.moduleName.set("io.opentelemetry.api.audit")

dependencies {
  api(project(":api:all"))
  api(project(":context"))
}

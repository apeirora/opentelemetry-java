plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("otel.animalsniffer-conventions")
}

description = "OpenTelemetry OTLP Audit Exporter"
otelJava.moduleName.set("io.opentelemetry.exporter.otlp.audit")

dependencies {
  api(project(":sdk:audit"))
  api(project(":sdk:logs"))
  implementation(project(":exporters:otlp:all"))
  implementation(project(":exporters:otlp:common"))
  implementation(project(":exporters:sender:okhttp"))

  testImplementation(project(":sdk:audit"))
  testImplementation(project(":sdk:logs"))
}

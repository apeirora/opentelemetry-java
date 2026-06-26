plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("otel.animalsniffer-conventions")
}

description = "OpenTelemetry Audit Logging SDK"
otelJava.moduleName.set("io.opentelemetry.sdk.audit")
otelJava.osgiUnversionedOptionalPackages.add("audit.log")

dependencies {
  api(project(":api:all"))
  api(project(":sdk:common"))
  api(project(":sdk:logs"))
  api("eu.apeirora:audit.log")

  annotationProcessor("com.google.auto.value:auto-value")

  testImplementation(project(":sdk:testing"))

  testImplementation("org.awaitility:awaitility")
  testImplementation("com.google.guava:guava")
}

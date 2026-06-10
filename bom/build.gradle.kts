plugins {
  id("otel.bom-conventions")
}

description = "OpenTelemetry Bill of Materials"
group = "eu.apeirora.opentelemetry"
base.archivesName.set("opentelemetry-bom")

otelBom.projectFilter.set { !it.hasProperty("otel.release") }

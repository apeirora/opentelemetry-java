subprojects {
  // https://github.com/gradle/gradle/issues/847
  group = "eu.apeirora.opentelemetry.exporters"
  val proj = this
  plugins.withId("java") {
    configure<BasePluginExtension> {
      archivesName.set("opentelemetry-exporter-${proj.name}")
    }
  }
}

subprojects {
  // Workaround https://github.com/gradle/gradle/issues/847
  group = "eu.apeirora.opentelemetry.exporter.httpsender"
  val proj = this
  plugins.withId("java") {
    configure<BasePluginExtension> {
      archivesName.set("opentelemetry-exporter-sender-${proj.name}")
    }
  }
}

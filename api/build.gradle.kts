subprojects {
  // Workaround https://github.com/gradle/gradle/issues/847
  group = "eu.apeirora.opentelemetry.api"
  val proj = this
  plugins.withId("java") {
    configure<BasePluginExtension> {
      archivesName.set("opentelemetry-api-${proj.name}")
    }
  }
}

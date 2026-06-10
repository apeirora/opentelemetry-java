subprojects {
  // Workaround https://github.com/gradle/gradle/issues/847
  group = "eu.apeirora.opentelemetry.extensions"
  val proj = this
  plugins.withId("java") {
    configure<BasePluginExtension> {
      archivesName.set("opentelemetry-extension-${proj.name}")
    }
  }
}

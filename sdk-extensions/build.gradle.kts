subprojects {
  // Workaround https://github.com/gradle/gradle/issues/847
  group = "eu.apeirora.opentelemetry.sdk.extensions"
  val proj = this
  plugins.withId("java") {
    configure<BasePluginExtension> {
      archivesName.set("opentelemetry-sdk-extension-${proj.name}")
    }
  }
}

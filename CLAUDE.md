# opentelemetry-java — Development Notes

## Language

- AI conversation: always respond in English
- Code, code comments, documentation: always write in US English

## Before creating a git commit

Run `./gradlew jApiCmp` as the last step before committing **when Kotlin sources changed**.
This updates the API diff files in `docs/apidiffs/current_vs_latest/` and must be included in
the commit. CI enforces that these files are up to date and will fail if they are stale.
YAML, Markdown, and workflow-only changes do not require running `jApiCmp`.

## Publishing Architecture

`otel.publish-conventions.gradle.kts` (in `buildSrc/`) defines the Maven publication for all
subprojects via the `maven-publish` convention plugin.

Sonatype/Maven Central is wired via `nexusPublishing` in the root `build.gradle.kts` — not via
a `repositories {}` block in the convention plugin. Additional publish targets (e.g. GitHub
Packages) must be added inside `publishing { }` in the convention plugin.

The signing block activates when the `CI` env var is set. Omit `CI` in a workflow to suppress
signing without removing GPG secrets.

Version is always `1.64.0-SNAPSHOT` while `snapshot = true` in `version.gradle.kts`.


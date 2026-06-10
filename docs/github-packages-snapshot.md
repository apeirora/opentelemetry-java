# Consuming SNAPSHOT Artifacts from GitHub Packages

SNAPSHOT builds of this project are published to GitHub Packages on every push
to `main` and `auditing`. The group ID is `eu.apeirora.opentelemetry` and the
version is `1.64.0-SNAPSHOT` (or whatever `version.gradle.kts` currently sets).

## Prerequisites

You need a GitHub personal access token (PAT) with the `read:packages` scope.
Create one at https://github.com/settings/tokens.

## Maven

**`~/.m2/settings.xml`** — add a server entry:

```xml
<settings>
  <servers>
    <server>
      <id>github-apeirora</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

**`pom.xml`** — add the repository and declare the dependency:

```xml
<repositories>
  <repository>
    <id>github-apeirora</id>
    <url>https://maven.pkg.github.com/apeirora/opentelemetry-java</url>
    <snapshots><enabled>true</enabled></snapshots>
    <releases><enabled>false</enabled></releases>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>eu.apeirora.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
    <version>1.64.0-SNAPSHOT</version>
  </dependency>
</dependencies>
```

Replace `opentelemetry-api` with the artifact you need. All published artifact
IDs follow the pattern `opentelemetry-<module>`.

## Gradle (Kotlin DSL)

**`~/.gradle/gradle.properties`** — store credentials outside the project:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

**`build.gradle.kts`** — add the repository:

```kotlin
repositories {
    maven {
        name = "GitHubApeirora"
        url = uri("https://maven.pkg.github.com/apeirora/opentelemetry-java")
        credentials {
            username = providers.gradleProperty("gpr.user")
                .orElse(providers.environmentVariable("GITHUB_ACTOR")).get()
            password = providers.gradleProperty("gpr.key")
                .orElse(providers.environmentVariable("GITHUB_TOKEN")).get()
        }
    }
}

dependencies {
    implementation("eu.apeirora.opentelemetry:opentelemetry-api:1.64.0-SNAPSHOT")
}
```

## Using in CI (GitHub Actions)

When your consumer project itself runs on GitHub Actions, `GITHUB_TOKEN` is
automatically available — no PAT needed:

```yaml
- name: Build
  run: mvn verify   # or ./gradlew build
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    GITHUB_ACTOR: ${{ github.actor }}
```

For Gradle, the repository block above already reads `GITHUB_TOKEN` from the
environment, so no further configuration is required.

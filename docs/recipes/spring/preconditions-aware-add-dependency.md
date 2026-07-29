### `pl.allegro.tech.allwrite.recipes.spring.PreconditionsAwareAddDependency` { data-toc-label="PreconditionsAwareAddDependency" }

Adds a Gradle dependency when code uses any configured fully qualified type or when any configured Gradle dependency is present. It supports Gradle build files and `gradle/libs.versions.toml` version catalogs.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `requiredClasspath` | `List<String>` | No | Classpath entries required to parse the configured types. |
| `requiredTypes` | `List<String>` | No | Fully qualified types whose usage triggers dependency insertion. |
| `requiredDependencies` | `List<String>` | No | Gradle dependencies whose presence triggers dependency insertion, in `groupId:artifactId` format. |
| `configuration` | `String` | Yes | Gradle configuration to add the dependency to. |
| `groupId` | `String` | Yes | Group ID of the dependency to add. |
| `artifactId` | `String` | Yes | Artifact ID of the dependency to add. |
| `versionCatalogName` | `String` | No | Dependency name in the Gradle version catalog. Defaults to `artifactId`. |

Configure at least one of `requiredTypes` or `requiredDependencies`.

With `requiredTypes = ["com.example.Feature"]`, `configuration = "implementation"`, `groupId = "com.example"`, and `artifactId = "feature"`:

Before:
```kotlin
import com.example.Feature

class Example(val feature: Feature)
```

```kotlin
dependencies {
}
```

After:
```kotlin
dependencies {
    implementation("com.example:feature")
}
```

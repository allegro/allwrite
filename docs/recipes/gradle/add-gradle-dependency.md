### `pl.allegro.tech.allwrite.recipes.gradle.AddGradleDependency` { data-toc-label="AddGradleDependency" }

A two-pass scanning recipe that adds a dependency to a Gradle project. In the scan phase, it parses the TOML version catalog (`gradle/libs.versions.toml`) and discovers module roots. In the transform phase, it adds the library entry to the version catalog and a dependency reference in `build.gradle(.kts)`.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `configuration` | `String` | Yes | Gradle configuration (e.g. `implementation`, `testImplementation`). |
| `groupId` | `String` | Yes | Dependency group ID. |
| `artifactId` | `String` | Yes | Dependency artifact ID. |
| `version` | `String` | No | Dependency version. Used when no version catalog is present. |
| `versionCatalogName` | `String` | No | Alias for the dependency in the version catalog. Auto-generated from coordinates if not specified. |

Before (with `configuration = "testRuntimeOnly"`, `groupId = "org.junit.platform"`, `artifactId = "junit-platform-launcher"`):

`gradle/libs.versions.toml`:
```toml
[libraries]
mylib-starter = { group = "com.example.lib", name = "mylib-starter" }
```

`build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.test)
}
```

After:

`gradle/libs.versions.toml`:
```toml
[libraries]
mylib-starter = { group = "com.example.lib", name = "mylib-starter" }
junit-platform-launcher = { group = "org.junit.platform", name = "junit-platform-launcher" }
```

`build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
```


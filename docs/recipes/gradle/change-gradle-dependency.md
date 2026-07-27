### `pl.allegro.tech.allwrite.recipes.gradle.ChangeGradleDependency` { data-toc-label="ChangeGradleDependency" }

Changes a Gradle dependency's group ID, artifact ID, and optional version. It supports `build.gradle`, `build.gradle.kts`, and `gradle/libs.versions.toml`, updating matching version-catalog entries and refs when present. The old group and artifact IDs accept glob patterns, so `*` can match any coordinate segment.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `oldGroupId` | `String` | Yes | Dependency group ID to replace. Accepts glob patterns. |
| `oldArtifactId` | `String` | Yes | Dependency artifact ID to replace. Accepts glob patterns. |
| `newGroupId` | `String` | No | Replacement group ID. Defaults to the old group ID. |
| `newArtifactId` | `String` | No | Replacement artifact ID. Defaults to the old artifact ID. |
| `newVersion` | `String` | No | Replacement version. If omitted, the version is removed. |

Before (with `oldGroupId = "com.fasterxml.jackson.module"`, `oldArtifactId = "jackson-module-afterburner"`):

`build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2")
}
```

After:

`build.gradle.kts`:
```kotlin
dependencies {
    implementation("tools.jackson.module:jackson-module-blackbird:3.1.4")
}
```


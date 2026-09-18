### `pl.allegro.tech.allwrite.recipes.gradle.RemoveLibraryVersionRefs` { data-toc-label="RemoveLibraryVersionRefs" }

Removes `version.ref` from library aliases equal to `pluginName` or matching `pluginName-*` in `gradle/libs.versions.toml`. It does not add plugins or modify
Gradle build files.

Options:

| Name                         | Type     | Required | Description                                                                                    |
|------------------------------|----------|----------|------------------------------------------------------------------------------------------------|
| `pluginName`                 | `String` | Yes      | Alias and prefix of entries whose references are removed.                                      |
| `applyToModulesWithPluginId` | `String` | No       | Limits the catalog edit to cases where every consuming module applies the specified plugin ID. |

Before (with `pluginName = "example"`):

```toml
[versions]
example = "1.2.3"
other = "4.5.6"

[libraries]
example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
other = { group = "com.other", name = "other", version.ref = "other" }
```

After:

```toml
[versions]
example = "1.2.3"
other = "4.5.6"

[libraries]
example-bom = { group = "com.example", name = "example-bom" }
other = { group = "com.other", name = "other", version.ref = "other" }
```

When `applyToModulesWithPluginId` is set, the recipe scans all Gradle build files that reference matching catalog
aliases, using either catalog accessors or literal `group:name` coordinates. It removes `version.ref` only when at
least one build file applies the configured plugin and every build file referencing a matching library applies it.
If any referencing module does not apply the plugin, the recipe leaves the catalog unchanged.

For example, with `applyToModulesWithPluginId = "application"`, the following modules cause a complete no-op because
`lib/build.gradle.kts` references the matching alias without applying `application`:

`app/build.gradle.kts`:

```kotlin
plugins {
    application
}

dependencies {
    implementation(libs.example.bom)
}
```

`lib/build.gradle.kts`:

```kotlin
plugins {
    `java-library`
}

dependencies {
    implementation(libs.example.bom)
}
```

The catalog remains unchanged:

```toml
[libraries]
example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
```

**Note:** Bundle references are not supported when the option is set. If a matching alias appears in a `[bundles]` entry, the
recipe performs a complete no-op rather than attempting to rewrite the bundle. The limitation is intentional because
bundle usage cannot be attributed to a specific build file. When the option is omitted, the existing ungated behavior
is unchanged.

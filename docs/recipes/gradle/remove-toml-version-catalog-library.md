### `pl.allegro.tech.allwrite.recipes.gradle.RemoveTomlVersionCatalogLibrary` { data-toc-label="RemoveTomlVersionCatalogLibrary" }

Removes matching library dependency declarations from Gradle build files and, when present, the corresponding library
alias and unused version from `gradle/libs.versions.toml`.
The library is matched by its group and artifact coordinates, rather than by its version-catalog alias.

Options:

| Name                         | Type     | Required | Description                                                   |
|------------------------------|----------|----------|---------------------------------------------------------------|
| `groupId`                    | `String` | Yes      | Group ID of the library to remove.                            |
| `artifactId`                 | `String` | Yes      | Artifact ID of the library to remove.                         |
| `applyToModulesWithPluginId` | `String` | No       | Restrict build-file edits to modules applying this plugin ID. |

The recipe supports library entries using either `group` and `name` or `module` notation. It removes catalog
accessor references and literal GAV references from dependency declarations, including `platform(...)` and
`enforcedPlatform(...)` wrappers, in both Kotlin and Groovy Gradle DSLs. Literal GAV references are removed even when
the target library is not present in the version catalog; in that case, no catalog changes are made.

Before (with `groupId = "com.example"` and `artifactId = "example-bom"`):

```toml
[versions]
example = "1.2.3"
other = "4.5.6"

[libraries]
example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
other = { group = "com.other", name = "other", version.ref = "other" }
```

```kotlin
dependencies {
    implementation(platform(libs.example.bom))
    implementation(libs.other)
}
```

After:

```toml
[versions]
other = "4.5.6"

[libraries]
other = { group = "com.other", name = "other", version.ref = "other" }
```

```kotlin
dependencies {
    implementation(libs.other)
}
```

When `applyToModulesWithPluginId` is configured, dependency declarations are removed independently from matching
modules. If another module references the library without applying the configured plugin, the shared catalog alias and
its version entries are kept so that module remains valid. Build files that do not apply the configured plugin are left
unchanged.

For example, with `applyToModulesWithPluginId = "application"`:

Before:

`gradle/libs.versions.toml`

```toml
[versions]
example = "1.2.3"
other = "4.5.6"

[libraries]
example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
other = { group = "com.other", name = "other", version.ref = "other" }
```

`app/build.gradle.kts`

```kotlin
plugins {
    application
}

dependencies {
    implementation(platform(libs.example.bom))
    implementation(libs.other)
}
```

`lib/build.gradle.kts`

```kotlin
plugins {
    id("java-library")
}

dependencies {
    implementation(platform(libs.example.bom))
    implementation(libs.other)
}
```

After:

`gradle/libs.versions.toml`

```toml
[versions]
example = "1.2.3"
other = "4.5.6"

[libraries]
example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
other = { group = "com.other", name = "other", version.ref = "other" }
```

`app/build.gradle.kts`

```kotlin
plugins {
    application
}

dependencies {
    implementation(libs.other)
}
```

`lib/build.gradle.kts`

```kotlin
plugins {
    id("java-library")
}

dependencies {
    implementation(platform(libs.example.bom))
    implementation(libs.other)
}
```

The dependency is removed from the application module, while the library module remains unchanged. Because the library
module still references `libs.example.bom`, the shared catalog alias and its version entry are retained.

The `[versions]` entry used by a removed alias is removed only when no other library, plugin, or direct `libs.versions.*`
accessor refers to it. For example, the version remains when a plugin still uses it:

Before:

```toml
[versions]
example = "1.2.3"
other = "4.5.6"

[libraries]
example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
other = { group = "com.other", name = "other", version.ref = "other" }

[plugins]
example = { id = "com.example.plugin", version.ref = "example" }
other = { id = "com.other.plugin", version.ref = "other" }
```

After removing `example-bom`:

```toml
[versions]
example = "1.2.3"
other = "4.5.6"

[libraries]
other = { group = "com.other", name = "other", version.ref = "other" }

[plugins]
example = { id = "com.example.plugin", version.ref = "example" }
other = { id = "com.other.plugin", version.ref = "other" }
```

**Note:** Removing aliases referenced by `[bundles]` is not supported yet. If the target alias belongs to a bundle, the recipe
leaves the catalog and all build files unchanged.

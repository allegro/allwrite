### `pl.allegro.tech.allwrite.recipes.gradle.AddTomlVersionCatalogPlugin` { data-toc-label="AddTomlVersionCatalogPlugin" }

Adds or updates a plugin entry in `gradle/libs.versions.toml` and applies the corresponding catalog alias to `build.gradle` and `build.gradle.kts` plugin blocks. Library entries and their `version.ref` values are not changed.

When the plugin ID is already present under another alias, the recipe reuses that alias. It fails rather than overwriting an alias that belongs to a different plugin or guessing which alias to use when the same plugin ID appears more than once.

Options:

| Name            | Type     | Required | Description                                      |
|-----------------|----------|----------|--------------------------------------------------|
| `pluginName`    | `String` | Yes      | Version catalog alias for the plugin.            |
| `pluginId`      | `String` | Yes      | Gradle plugin ID.                                |
| `pluginVersion` | `String` | Yes      | Plugin version to add or set.                    |

Before (with `pluginName = "example"`, `pluginId = "com.example.plugin"`, and `pluginVersion = "1.2.3"`):

```toml
[libraries]
example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }
```

After:

```toml
[libraries]
example-bom = { group = "com.example", name = "example-bom", version.ref = "example" }

[plugins]
example = { id = "com.example.plugin", version = "1.2.3" }
```

```kotlin
plugins {
    alias(libs.plugins.example)
}
```

### `pl.allegro.tech.allwrite.recipes.gradle.ReplaceLibraryVersionRefsWithPlugin` { data-toc-label="ReplaceLibraryVersionRefsWithPlugin" }

Replaces matching library version references with a versioned plugin in `gradle/libs.versions.toml`. The recipe removes `version.ref` from library aliases equal to `pluginName` or matching `pluginName-*`, then adds or updates the plugin entry. Unrelated library version references are preserved.

Options:

| Name            | Type     | Required | Description                                                                       |
|-----------------|----------|----------|-----------------------------------------------------------------------------------|
| `pluginName`    | `String` | Yes      | Version catalog alias for the plugin and prefix of the library aliases to update. |
| `pluginId`      | `String` | Yes      | Gradle plugin ID.                                                                 |
| `pluginVersion` | `String` | Yes      | Plugin version to add or set.                                                     |

Before (with `pluginName = "example"`, `pluginId = "com.example.plugin"`, and `pluginVersion = "1.2.3"`):

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

[plugins]
example = { id = "com.example.plugin", version = "1.2.3" }
```

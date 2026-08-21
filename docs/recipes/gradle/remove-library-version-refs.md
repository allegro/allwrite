### `pl.allegro.tech.allwrite.recipes.gradle.RemoveLibraryVersionRefs` { data-toc-label="RemoveLibraryVersionRefs" }

Removes `version.ref` from library aliases equal to `pluginName` or matching `pluginName-*` in `gradle/libs.versions.toml`. It does not add plugins or modify Gradle build files.

Options:

| Name         | Type     | Required | Description                                             |
|--------------|----------|----------|---------------------------------------------------------|
| `pluginName` | `String` | Yes      | Alias and prefix of entries whose references are removed. |

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

### `pl.allegro.tech.allwrite.recipes.gradle.AddSettingsPluginRepository` { data-toc-label="AddSettingsPluginRepository" }

Wraps the OpenRewrite `org.openrewrite.gradle.plugins.AddSettingsPluginRepository` recipe and adds an optional gate that
restricts the repository change to repositories where a specified plugin is applied. Without the gate, it behaves like the
upstream recipe.

Options:

| Name                         | Type      | Required | Description                                                                              |
|------------------------------|-----------|----------|------------------------------------------------------------------------------------------|
| `type`                       | `String`  | Yes      | Gradle repository method to add, such as `maven` or `gradlePluginPortal`.                |
| `url`                        | `String?` | No       | Repository URL. Required by URL-based repositories and omitted for `gradlePluginPortal`. |
| `applyToModulesWithPluginId` | `String?` | No       | Only add the repository when at least one build file applies this plugin ID.             |

With `type = "maven"` and `url = "https://repo.example.com"`:

Before:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
```

After:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://repo.example.com")
        }
    }
}
```

When `applyToModulesWithPluginId` is configured, all `build.gradle` and `build.gradle.kts` files are checked. The repository
is added to the repository-level settings file if any build file applies the configured plugin. If no build file applies it,
the settings file remains unchanged.

For `type = "gradlePluginPortal"`, set `url` to `null`.

### `pl.allegro.tech.allwrite.recipes.gradle.AddPluginManagementRepositories` { data-toc-label="AddPluginManagementRepositories" }

Adds a configured Maven repository and `gradlePluginPortal()` to the `pluginManagement.repositories` block in `settings.gradle` or `settings.gradle.kts`.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `url` | `String` | Yes | URL of the Maven repository to add. |

Configure the recipe in a composite recipe:

```yaml
type: specs.openrewrite.org/v1beta/recipe
name: com.example.AddPluginManagementRepositories
displayName: Add plugin management repositories
description: Adds the configured Maven repository and Gradle Plugin Portal to plugin management.
recipeList:
  - pl.allegro.tech.allwrite.recipes.gradle.AddPluginManagementRepositories:
      url: https://repo.example.com/maven
```

For `settings.gradle.kts`, the result is:

```kotlin
pluginManagement {
    repositories {
        maven {
            url = uri("https://repo.example.com/maven")
        }
        gradlePluginPortal()
    }
}
```

For `settings.gradle`, OpenRewrite uses the equivalent Groovy syntax `url = "https://repo.example.com/maven"`.

# Contributing guide

## How to write a recipe?

All recipe implementations are kept in the `allwrite-recipes` module.

In general, you should follow the official [Authoring Recipes](https://docs.openrewrite.org/authoring-recipes) docs from OpenRewrite.

However, `allwrite` has some custom features that you can use.

### Visibility

Every recipe within `allwrite-recipes` must provide the `visibility:[internal/public]` tag. Public recipes will be presented to the user via
`allwrite ls` command.

### Friendly names

Every public recipe must provide a friendly name in the form of 2 tags:
* `group:<someGroup>`
* `action:<someAction>`

For example, the following set of tags [`group:workflows`, `action:introduceSetupGradle`] will result in a recipe that can be executed like that:
```
allwrite run workflows/introduceSetupGradle
```

### Convenient base classes

For convenience, you can extend either `AllwriteRecipe` or `AllwriteScanningRecipe` (from the `allwrite-spi` module). They will build all required tags for you:
```kotlin
class SomeRecipe : AllwriteRecipe(
    displayName = "Some recipe", // optional, defaults to class name
    description = "Some description.", // optional, defaults to displayName + '.'
    visibility = PUBLIC, // optional, defaults to INTERNAL
    group = "some-group", // required if the visibility is PUBLIC
    action = "some-action" // required if the visibility is PUBLIC
) {
    // your implementation
}
```

### Dependabot integration

If your recipe should be triggered automatically when Dependabot bumps a specific dependency, declare `dependabotArtifacts`:
```kotlin
class SomeMigrationRecipe : AllwriteRecipe(
    visibility = PUBLIC,
    group = "some-group",
    action = "upgrade",
    dependabotArtifacts = listOf("com.example:some-library"),
) {
    // your implementation
}
```

For declarative YAML recipes, add `dependabot-artifact:<coordinates>` tags:
```yaml
tags:
  - visibility:public
  - group:some-group
  - action:upgrade
  - dependabot-artifact:com.example:some-library
```

When `allwrite run-dependabot` processes a Dependabot PR that bumps `com.example:some-library`, it will dynamically match and run all recipes that declare this artifact tag (and match the version range).

### Limiting which files should be parsed

> [!TIP]
> It may be very useful for improving performance and overcoming OpenRewrite issues with parsing Groovy files

If your recipe is only interested in very specific files (for example it only modifies the `tycho.yaml` file) you can implement the `ParsingAwareRecipe`
interface:
```kotlin
class SomeRecipe : AllwriteRecipe(visibility = INTERNAL), ParsingAwareRecipe {

    override fun selectFilesToParse(inputFiles: List<Path>): List<Path> {
        // return the files to be parsed
    }
}
```

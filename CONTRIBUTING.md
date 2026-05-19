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
* `recipe:<someRecipe>`

For example, the following set of tags [`group:workflows`, `recipe:introduceSetupGradle`] will result in a recipe that can be executed like that:
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

## Architecture

The `allwrite` is a modular project, utilizing dependency injection capabilities from the [Koin](https://github.com/InsertKoinIO/koin) framework.

It consists of the following Gradle modules (that may contain one or more Koin modules):
* `allwrite-api` - published API with incoming port interfaces (`RecipeExecutor`, `RecipeSource`, `RecipeCoordinates`) for interacting with `allwrite-runtime`
* `allwrite-spi` - published SPI with base classes for recipe authors (`AllwriteRecipe`, `AllwriteScanningRecipe`, `RecipeMetadata`)
* `allwrite-cli` - provides both Application and Infrastructure layers for the CLI app
* `allwrite-runtime` - provides core implementation (implements `allwrite-api` interfaces); equivalent of the Domain layer
* `allwrite-recipes` - contains OpenRewrite recipes to be executed by `allwrite-cli`
* `allwrite-completions` - provides annotation processors generating CLI auto-completions

The below diagram shows Koin modules, not Gradle modules. The rule of thumb is: we don't extract Koin module as a separate Gradle module unless required
by the build logic. For example, `allwrite-runtime` must be Gradle module, because it is used by both `allwrite-cli` (at app runtime)
and `allwrite-completions` (at app compile time).

Please keep the diagram up-to-date by editing `architecture.puml` file with the help of [PlantUML IntelliJ Plugin](https://plugins.jetbrains.com/plugin/7017-plantuml-integration) and replacing the rendered PNG.

If you don't like installing IntelliJ plugins, edit `architecture.puml` and execute the following command:
```bash
docker run --rm -v $(pwd):/app -w /app ghcr.io/plantuml/plantuml plantuml docs/architecture.puml
```

![](docs/architecture.png)

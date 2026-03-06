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

For convenience, you can extend either `AllegroRecipe` or `AllegroScanningRecipe`. It will build all required tags for you:
```kotlin
class SomeRecipe : AllegroRecipe(
    displayName = "Some recipe", // optional, defaults to class name
    description = "Some description.", // optional, defaults to displayName + '.'
    visibility = PUBLIC, // optional, defaults to INTERNAL
    group = "some-group", // required if the visibility is PUBLIC
    recipe = "some-recipe" // required if the visibility is PUBLIC
) {
    // your implementation
}
```

### Limiting which files should be parsed

> [!TIP]
> It may be very useful for improving performance and overcoming OpenRewrite issues with parsing Groovy files

If your recipe is only interested in very specific files (for example it only modifies the `tycho.yaml` file) you can implement the `ParsingAwareRecipe`
interface:
```kotlin
class SomeRecipe : AllegroRecipe(), ParsingAwareRecipe {

    override fun selectFilesToParse(inputFiles: List<Path>): List<Path> {
        // return the files to be parsed
    }
}
```

## Architecture

The `allwrite` is a modular project, utilizing dependency injection capabilities from the [Koin](https://github.com/InsertKoinIO/koin) framework.

It consists of the following Gradle modules (that may contain one or more Koin modules):
* `allwrite-runner` - provides both Application and Infrastructure layers for the CLI app
* `allwrite-runtime` - provides core implementation interacting directly with the OpenRewrite runtime; equivalent of the Domain layer
* `allwrite-recipes` - contains OpenRewrite recipes to be executed by `allwrite-runner`
* `allwrite-completions` - provides annotation processors generating CLI auto-completions

The below diagram shows Koin modules, not Gradle modules. The rule of thumb is: we don't extract Koin module as a separate Gradle module unless required
by the build logic. For example, `allwrite-runtime` must be Gradle module, because it is used by both `allwrite-runner` (at app runtime)
and `allwrite-completions` (at app compile time).

Please keep the diagram up-to-date by editing `architecture.puml` file with the help of [PlantUML IntelliJ Plugin](https://plugins.jetbrains.com/plugin/7017-plantuml-integration) and replacing the rendered PNG.

If you don't like installing IntelliJ plugins, edit `architecture.puml` and execute the following command:
```bash
docker run --rm -v $(pwd):/app -w /app ghcr.io/plantuml/plantuml plantuml docs/architecture.puml
```

![](docs/architecture.png)

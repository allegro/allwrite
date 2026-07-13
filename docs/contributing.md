# Writing recipes

All recipe implementations live in the `allwrite-recipes` module.

Follow the official [OpenRewrite authoring guide](https://docs.openrewrite.org/authoring-recipes) first, then use the allwrite-specific helpers below when needed.

## Visibility and naming

Every recipe must declare `visibility:internal` or `visibility:public`.

Public recipes need a friendly name made of:

- `group:<someGroup>`
- `action:<someAction>`

This is what makes `allwrite run workflows/introduceSetupGradle` possible.

## Base classes

Prefer the provided base classes when you can:

```kotlin
class SomeRecipe : AllwriteRecipe(
    displayName = "Some recipe",
    description = "Some description.",
    visibility = PUBLIC,
    group = "some-group",
    action = "some-action",
) {
}
```

`AllwriteScanningRecipe` offers the same metadata support for scanning recipes.

## Parsing-aware recipes

Implement `ParsingAwareRecipe` when a recipe only needs a subset of input files parsed.

```kotlin
class SomeRecipe : AllwriteRecipe(visibility = INTERNAL), ParsingAwareRecipe {
    override fun selectFilesToParse(inputFiles: List<Path>): List<Path> = inputFiles
}
```

This is useful when a recipe targets a specific file set or when parsing less content improves performance.

## Classpath-aware recipes

Implement `ClasspathAwareRecipe` when a recipe needs extra artifacts on the parsing classpath.

```kotlin
class SomeRecipe : AllwriteRecipe(visibility = INTERNAL), ClasspathAwareRecipe {
    override fun requireOnClasspath(): List<String> = listOf("spring-web-6", "spring-core-6")
}
```

allwrite uses this information to split execution into isolated phases and to resolve the matching recipe classpath before parsing.

## Dependabot integration

Use `dependabotArtifacts` on Kotlin recipes or `dependabot-artifact:<coordinates>` tags in YAML recipes when a recipe should run for matching Dependabot updates.

## Module map

- `allwrite-api` exposes incoming ports
- `allwrite-spi` exposes recipe authoring helpers
- `allwrite-runtime` contains execution logic
- `allwrite-recipes` contains the built-in recipes

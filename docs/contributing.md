# Contributing guide

## How to write a recipe?

All recipe implementations are kept in the `allwrite-recipes` module.

In general, you should follow the official [Authoring Recipes](https://docs.openrewrite.org/authoring-recipes) docs from OpenRewrite.

However, `allwrite` has some custom features that you can use.

### Choosing a recipe base class

Choose the base class according to how authors should invoke the recipe:

| Recipe type | Base class | Discovery | Invocation |
| --- | --- | --- | --- |
| Regular recipe | `AllwriteRecipe` | `allwrite ls --all` | Fully-qualified recipe name |
| Scanning recipe | `AllwriteScanningRecipe` | `allwrite ls --all` | Fully-qualified recipe name |
| User-facing CLI operation | `CliAllwriteRecipe` | `allwrite ls` | Friendly name |

`allwrite ls` lists recipes that define both `group` and `action` tags. Reserve those tags for recipes that need no OpenRewrite recipe
options: friendly-name execution accepts only the recipe name and optional version range. Recipes that need options should omit the
tags, appear in `allwrite ls --all`, and be invoked by their fully-qualified name.

Use `AllwriteRecipe` for a focused transformation that is normally included in a larger migration or invoked by its fully-qualified name:

```kotlin
class ReplaceDeprecatedApi : AllwriteRecipe(
    displayName = "Replace deprecated API",
    description = "Replaces the deprecated API with its supported alternative.",
) {
    override fun getVisitor(): TreeVisitor<*, ExecutionContext> = TODO()
}
```

Use `AllwriteScanningRecipe` when the recipe needs to collect information before changing source files:

```kotlin
class ConsolidateConfiguration : AllwriteScanningRecipe<MutableSet<String>>(
    displayName = "Consolidate configuration",
    description = "Consolidates duplicate configuration entries.",
) {
    override fun getInitialValue(ctx: ExecutionContext): MutableSet<String> = mutableSetOf()
}
```

### Creating a CLI recipe

Use `CliAllwriteRecipe` for an intentional, user-facing operation such as a framework upgrade. It requires nonblank `group` and `action`
arguments and generates the corresponding tags automatically. Use it only for recipes without OpenRewrite recipe options.

```kotlin
class UpgradeExampleLibrary : CliAllwriteRecipe(
    group = "some-group",
    action = "upgrade",
    displayName = "Upgrade Example Library",
    description = "Migrates a project to Example Library 2.",
    from = "1",
    to = "2",
) {
    override fun getVisitor(): TreeVisitor<*, ExecutionContext> = TODO()
}
```

The example is listed as `some-group/upgrade 1 2` and can be run with:

```shell
allwrite run some-group/upgrade 1 2
```

The optional `from` and `to` values describe a migration range. They are included in listings and allow `allwrite run` to select a
matching versioned recipe. Use them for upgrade or migration recipes; omit them for operations with no version range.

### Declaring a CLI recipe in YAML

Declarative YAML recipes do not extend Kotlin base classes. Add both `group` and `action` tags to a recipe without options to make one
appear in `allwrite ls`:

```yaml
type: specs.openrewrite.org/v1beta/recipe
name: com.example.UpgradeExampleLibrary
displayName: Upgrade Example Library
description: Migrates a project to Example Library 2.
tags:
  - group:some-group
  - action:upgrade
  - from:1
  - to:2
recipeList:
  - org.openrewrite.java.ChangeType:
      oldFullyQualifiedTypeName: com.example.LegacyType
      newFullyQualifiedTypeName: com.example.CurrentType
```

Omit either tag for a recipe with options so it appears only in `allwrite ls --all`.

### Dependabot integration

If a recipe should run automatically when Dependabot bumps a dependency, declare `dependabotArtifacts`. This is independent of CLI
discovery: a recipe may have a friendly name, be available only by its fully-qualified name, or both.
```kotlin
class SomeMigrationRecipe : CliAllwriteRecipe(
    group = "some-group",
    action = "upgrade",
    dependabotArtifacts = listOf("com.example:some-library"),
) {
    // your implementation
}
```

For declarative YAML recipes, add `dependabot-artifact:<coordinates>`:
```yaml
tags:
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
class SomeRecipe : AllwriteRecipe(), ParsingAwareRecipe {

    override fun selectFilesToParse(inputFiles: List<Path>): List<Path> {
        // return the files to be parsed
    }
}
```

### Supplying a recipe classpath

Implement `ClasspathAwareRecipe` when a recipe needs additional artifacts on the parser classpath:

```kotlin
class SomeRecipe : AllwriteRecipe(), ClasspathAwareRecipe {

    override fun requireOnClasspath(): List<String> =
        listOf("spring-web-6", "spring-core-6")
}
```

allwrite resolves the requested classpath entries before parsing. Classpath-aware recipes are executed in isolated phases so recipes requiring different classpaths do not interfere with each other.

### Running postprocessing

Implement `PostprocessingRecipe` when work must run after OpenRewrite changes have been applied:

```kotlin
class SomeRecipe : AllwriteRecipe(), PostprocessingRecipe {

    override fun postprocess(): PostprocessingResult =
        PostprocessingResult.Success
}
```

Return `PostprocessingResult.Success` after successful postprocessing. Return `PostprocessingResult.Failure(errorMessage)` to fail the recipe execution and report the error.

## Working on the documentation

The documentation site is built with MkDocs. From the `docs/` directory, create a virtual environment and install the documentation dependencies:

```bash
python3 -m venv .venv
./.venv/bin/python -m pip install -r requirements.txt
```

Build the documentation locally with:

```bash
./.venv/bin/python -m mkdocs build --config-file ../mkdocs.yml --strict
```

To preview the documentation while editing, start the local development server:

```bash
./.venv/bin/python -m mkdocs serve --config-file ../mkdocs.yml
```

Then open [http://127.0.0.1:8000/](http://127.0.0.1:8000/) in a browser. MkDocs automatically rebuilds the site when source files change.

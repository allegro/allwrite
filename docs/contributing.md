# Contributing guide

## How to write a recipe?

All recipe implementations are kept in the `allwrite-recipes` module.

In general, you should follow the official [Authoring Recipes](https://docs.openrewrite.org/authoring-recipes) docs from OpenRewrite.

However, `allwrite` has some custom features that you can use.

### Friendly names

`allwrite ls` presents recipes with a group and action. All other recipes are available through `allwrite ls --all` and can always
be run using their full recipe ID. `CliAllwriteRecipe` requires a group and action, which form its friendly name:

- `group:<someGroup>`
- `action:<someAction>`

For example, the following set of tags [`group:workflows`, `action:introduceSetupGradle`] will result in a recipe that can be executed like that:
```
allwrite run workflows/introduceSetupGradle
```

### Convenient base classes

For convenience, extend `CliAllwriteRecipe` for recipes launched by a friendly name, or `AllwriteRecipe` and `AllwriteScanningRecipe`
for all other recipes:
```kotlin
class SomeRecipe : CliAllwriteRecipe(
    displayName = "Some recipe", // optional, defaults to class name
    description = "Some description.", // optional, defaults to displayName + '.'
    group = "some-group",
    action = "some-action",
) {
    // your implementation
}
```

### Dependabot integration

If your recipe should be triggered automatically when Dependabot bumps a specific dependency, declare `dependabotArtifacts`:
```kotlin
class SomeMigrationRecipe : CliAllwriteRecipe(
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

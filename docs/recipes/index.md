# Recipes

The following list covers all custom recipes provided by `allwrite`.

## Recipe authoring helpers

The `allwrite-spi` module provides the following public helpers for recipe authors:

| Helper                   | Purpose                                                                                    |
|--------------------------|--------------------------------------------------------------------------------------------|
| `AllwriteRecipe`         | Base class for regular recipes. Builds display metadata and allwrite tags.                 |
| `CliAllwriteRecipe`      | Base class for recipes listed by the CLI. Requires a group and action.                     |
| `AllwriteScanningRecipe` | Base class for OpenRewrite scanning recipes with the same metadata support.                |
| `RecipeMetadata`         | Builds display name, description, version, and Dependabot tags.                            |
| `ParsingAwareRecipe`     | Restricts the files parsed for a recipe.                                                   |
| `ClasspathAwareRecipe`   | Requests additional parser classpath entries and isolated execution.                       |
| `PostprocessingRecipe`   | Runs additional work after recipe changes are applied.                                     |
| `PostprocessingResult`   | Reports postprocessing success or a failure with an error message.                         |

See [Writing recipes](../contributing.md) for usage guidelines and examples.

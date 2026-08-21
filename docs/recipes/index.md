# Recipes

The following list covers all custom recipes provided by `allwrite`.

## Recipe authoring helpers

The `allwrite-spi` module provides the following public helpers for recipe authors:

| Helper                        | Purpose                                                                                    |
|-------------------------------|--------------------------------------------------------------------------------------------|
| `AllwriteRecipe`              | Base class for regular recipes. They are available by fully-qualified name.                |
| `CliAllwriteRecipe`           | `AllwriteRecipe` for user-facing operations. Requires a group and action.                  |
| `AllwriteScanningRecipe`      | Base class for scanning recipes. They are available by fully-qualified name.               |
| `CliAllwriteScanningRecipe`   | `AllwriteScanningRecipe` for user-facing scanning operations. Requires a group and action. |
| `RecipeMetadata`              | Builds display name, description, version, and Dependabot tags.                            |
| `ParsingAwareRecipe`          | Restricts the files parsed for a recipe.                                                   |
| `ClasspathAwareRecipe`        | Requests additional parser classpath entries and isolated execution.                       |
| `PostprocessingRecipe`        | Runs additional work after recipe changes are applied.                                     |
| `PostprocessingResult`         | Reports postprocessing success or a failure with an error message.                         |

See [Writing recipes](../contributing.md) for usage guidelines and examples.

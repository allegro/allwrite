# Built-in recipes

## Recipe helpers

- `AllwriteRecipe` builds OpenRewrite metadata and tags for regular recipes
- `AllwriteScanningRecipe` does the same for scanning recipes
- `RecipeMetadata` derives display name, description, and tags
- `ParsingAwareRecipe` narrows the parsed file set before execution
- `ClasspathAwareRecipe` declares extra artifacts required on the recipe classpath

## YAML

- `pl.allegro.tech.allwrite.recipes.yaml.ExpandMappings` — expands flat YAML properties into nested mappings
- `pl.allegro.tech.allwrite.recipes.yaml.UnnestProperties` — removes one level of YAML nesting at a target path
- `pl.allegro.tech.allwrite.recipes.yaml.AddTopLevelLineBreaks` — inserts blank lines between top-level YAML entries
- `pl.allegro.tech.allwrite.recipes.yaml.DeleteProperty` — deletes YAML properties with anchor/comment-aware behavior
- `pl.allegro.tech.allwrite.recipes.yaml.FindKey` — fast case-insensitive YAML key search
- `pl.allegro.tech.allwrite.recipes.yaml.YamlEntryHasValue` — matches YAML entries with a specific value

## Gradle

- `pl.allegro.tech.allwrite.recipes.gradle.AddGradleDependency` — adds dependencies to Gradle projects and version catalogs
- `pl.allegro.tech.allwrite.recipes.gradle.ChangeGradleDependency` — changes dependency coordinates across Gradle files and TOML catalogs
- `pl.allegro.tech.allwrite.recipes.gradle.UpdateGradleDependency` — regex-based dependency version updates

## Java / Kotlin

- `pl.allegro.tech.allwrite.recipes.java.ChangeType` — replaces one type with another and can rename variables
- `pl.allegro.tech.allwrite.recipes.java.ChangeRecordField` — renames record fields across Java and Kotlin usages
- `pl.allegro.tech.allwrite.recipes.java.SimplifyMethodChain` — simplifies method chains, including Kotlin support
- `pl.allegro.tech.allwrite.recipes.java.RemoveUnusedPrivateFields` — removes unused private fields, optionally filtered by type
- `pl.allegro.tech.allwrite.recipes.java.RemoveUnusedImportsOfType` — removes unused imports for selected types
- `pl.allegro.tech.allwrite.recipes.java.ReplaceFactoryWithConstructor` — replaces factory calls with direct constructor invocations

## Spring

- `pl.allegro.tech.allwrite.recipes.spring.FindSpringProperty` — finds Spring properties in properties and YAML files
- `pl.allegro.tech.allwrite.recipes.spring.DeleteSpringProperty` — deletes a Spring property by key
- `pl.allegro.tech.allwrite.recipes.spring.DeleteSpringPropertyWithValue` — deletes a property only when it has a matching value
- `pl.allegro.tech.allwrite.recipes.spring.DeleteSpringPropertyFromSpringAnnotations` — removes annotation-based test properties
- `pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKey` — renames Spring property keys across files
- `pl.allegro.tech.allwrite.recipes.spring.RenameTaskExecutorBean` — adds the Spring Boot 3.5 task executor qualifier
- `pl.allegro.tech.allwrite.recipes.spring.AddNonNullableTypeBoundsToSpringRepositories` — adds Kotlin `: Any` bounds for Spring Data repositories
- `pl.allegro.tech.allwrite.recipes.spring.ReplaceStatusCodeValue` — replaces deprecated response status accessors

This page is the short reference view; the legacy root `RECIPES.md` now points here.

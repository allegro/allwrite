# allwrite

allwrite is a CLI tool and a collection of [OpenRewrite](https://docs.openrewrite.org/) recipes for automated code transformations across YAML, Gradle, Java/Kotlin, and Spring projects.

It wraps OpenRewrite's runtime into a CLI that can run recipes by name, integrate with GitHub Actions, and work with Dependabot pull requests.

## Why allwrite?

Use friendly recipe names instead of fully qualified Java class names, combine OpenRewrite's ecosystem with project-specific migrations, and package private recipes as external JARs.

## Project at a glance

- **Group ID:** `pl.allegro.tech.allwrite`
- **License:** Apache 2.0
- **JVM toolchain:** Java 21 (Temurin)

## What is included

`allwrite` is a modular project, utilizing dependency injection capabilities from the [Koin](https://github.com/InsertKoinIO/koin) framework.

It consists of the following Gradle modules (that may contain one or more Koin modules):

- **allwrite-api** - published API with incoming port interfaces (`RecipeExecutor`, `RecipeSource`, `RecipeCoordinates`) for interacting with `allwrite-runtime`
- **allwrite-spi** - published SPI with base classes for recipe authors (`AllwriteRecipe`, `AllwriteScanningRecipe`, `RecipeMetadata`)
- **allwrite-cli** - provides both Application and Infrastructure layers for the CLI app
- **allwrite-runtime** - provides core implementation (implements `allwrite-api` interfaces); equivalent of the Domain layer
- **allwrite-recipes** - contains OpenRewrite recipes to be executed by `allwrite-cli`
- **allwrite-completions** - provides annotation processors generating CLI auto-completions

## Quick links

- [CLI reference](cli.md)
- [Writing recipes](contributing.md)
- [Built-in recipes](recipes.md)

# allwrite

allwrite is a CLI tool and a collection of [OpenRewrite](https://docs.openrewrite.org/) recipes for automated code transformations across YAML, Gradle, Java/Kotlin, and Spring projects.

It wraps OpenRewrite's runtime into a CLI that can run recipes by name, integrate with GitHub Actions, and work with Dependabot pull requests.

- [Get started](cli.md){ .md-button .md-button--primary }
- [Write a recipe](contributing.md){ .md-button }

## Why allwrite?

Use friendly recipe names instead of fully qualified Java class names, combine OpenRewrite's ecosystem with project-specific migrations, and package private recipes as external JARs.

## Project at a glance

- **Group ID:** `pl.allegro.tech.allwrite`
- **License:** Apache 2.0
- **JVM toolchain:** Java 21 (Temurin)

## What is included

- `allwrite-cli` for running recipes and managing external recipe jars
- `allwrite-runtime` for recipe execution and source parsing
- `allwrite-recipes` for built-in OpenRewrite recipes
- `allwrite-spi` for recipe authoring helpers

## Quick links

- [CLI reference](cli.md)
- [Writing recipes](contributing.md)
- [Built-in recipes](recipes.md)

allwrite
========

A CLI tool and a collection of [OpenRewrite](https://docs.openrewrite.org/) recipes for automated code transformations across YAML, Gradle, Java/Kotlin, and Spring projects.

It wraps OpenRewrite's runtime into a user-friendly CLI that can run recipes by name, integrate with GitHub Actions, and work with Dependabot PRs.

## Usage

Installation:

```bash
brew install allwrite
```

Run a recipe by friendly name:

```bash
allwrite run springBoot/upgrade 3 4
```

Run a recipe by fully-qualified name:

```bash
allwrite run --recipe pl.allegro.tech.allwrite.recipes.SpringBoot4
```

## Recipes

The `allwrite` CLI comes with all the free OpenRewrite migrations bundled (Java/Kotlin refactoring, Spring Boot upgrades, etc.).

In addition, it provides a collection of custom recipes that aim to fill the gaps. See [RECIPES.md](RECIPES.md) for the full list.

If you're a library maintainer and want to automate the migration process for your users (or just have prepared a recipe that may be useful for others),
we would love to see your contribution! ❤️

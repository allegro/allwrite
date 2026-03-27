allwrite
========

What is `allwrite`? It's all you need for automatic code migrations!
- a CLI tool for running recipes via friendly names
- a collection of recipes, filling the gaps in vanilla [OpenRewrite](https://docs.openrewrite.org)
- a GitHub action integrating with Dependabot (coming soon)
- a GitHub workflow that can be externally orchestrated (coming soon)

## Usage

Installation:

```bash
brew tap allegro/tap
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

List available recipes:
```bash
allwrite ls
```

List all available recipes (the output will be huge):
```bash
allwrite ls -a
# or
allwrite ls --all
```

## Recipes

The `allwrite` CLI comes with all the free OpenRewrite migrations bundled (Java/Kotlin refactoring, Spring Boot upgrades, etc.).

In addition, it provides a collection of custom recipes that aim to fill the gaps. See [RECIPES.md](RECIPES.md) for the full list.

If you're a library maintainer and want to automate the migration process for your users (or just have prepared a recipe that may be useful for others),
we would love to see your contribution! ❤️

## External recipes

You can register external recipe JARs to extend `allwrite` with additional recipes from any URL.

Add an external recipe JAR:
```bash
allwrite external add custom-recipes https://repo.com/custom-recipes-1.0.0.jar
```

Update an external recipe JAR with a new URL:
```bash
allwrite external update custom-recipes https://repo.com/custom-recipes-2.0.0.jar
```

Re-fetch an external recipe JAR from its stored URL:
```bash
allwrite external update custom-recipes
```

List all registered external recipe JARs:
```bash
allwrite external ls
```

Remove an external recipe JAR:
```bash
allwrite external rm custom-recipes
```

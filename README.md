allwrite
========

What is `allwrite`? It's all you need for automatic code migrations!
- a CLI tool for running recipes via friendly names
- a collection of recipes, filling the gaps in vanilla [OpenRewrite](https://docs.openrewrite.org)
- a GitHub action integrating with Dependabot (coming soon)
- a GitHub bot automatically running migrations for Dependabot PRs (coming soon)

## Usage

Installation:

```bash
brew tap allegro/tap
brew install allwrite
```

Run a recipe by friendly name:

```bash
allwrite run springBoot/upgrade 3.5 4.0
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

You can add external recipes to your local `allwrite`, they can be closed-source and stored in a private artifact repository.

All you need to do: 
- package your recipes to a JAR file
- publish it somewhere
- grab the URL to the published JAR
- register that URL via `allwrite external add ...`

### Commands for external recipes

Add an external recipes JAR:
```bash
allwrite external add custom-recipes https://repo.com/custom-recipes-1.0.0.jar
```

Update an external recipes JAR with a new URL:
```bash
allwrite external update custom-recipes https://repo.com/custom-recipes-2.0.0.jar
```

Re-fetch an external recipes JAR (useful for SNAPSHOT versions):
```bash
allwrite external update custom-recipes
```

List external recipes JARs:
```bash
allwrite external ls
```

Remove an external recipes JAR:
```bash
allwrite external rm custom-recipes
```

### Local build

Install dist locally:

```bash
./gradlew :allwrite-cli:installDist
```

Run it via:

```sh
<allwrite-root>/allwrite-cli/build/installation/bin/allwrite <args>
```

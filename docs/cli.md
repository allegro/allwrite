# CLI reference

## Installation

```bash
brew tap allegro/tap
brew install allwrite
```

## Recipes

Run a recipe by friendly name:

```bash
allwrite run springBoot/upgrade 3.5 4.0
```

Run a recipe by fully qualified name:

```bash
allwrite run --recipe pl.allegro.tech.allwrite.recipes.SpringBoot4
```

List available recipes:

```bash
allwrite ls
```

List all available recipes:

```bash
allwrite ls -a
allwrite ls --all
```

## External recipes

```bash
allwrite external add custom-recipes https://repo.com/custom-recipes-1.0.0.jar
allwrite external update custom-recipes https://repo.com/custom-recipes-2.0.0.jar
allwrite external update custom-recipes
allwrite external ls
allwrite external rm custom-recipes
```

## Running from sources

```bash
./gradlew :allwrite-cli:run --args "run springBoot/upgrade 3 4"
./gradlew :allwrite-cli:run --args "run springBoot/upgrade 3 4" -Pworkdir=<path-to-projects>/some-project
./gradlew :allwrite-cli:installDist
<allwrite-root>/allwrite-cli/build/installation/bin/allwrite <args>
```

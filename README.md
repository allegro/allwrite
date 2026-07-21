# allwrite
allwrite is a CLI tool and a collection of [OpenRewrite](https://docs.openrewrite.org/) recipes for automated code transformations across YAML, Gradle, Java/Kotlin, and Spring projects.

It wraps OpenRewrite's runtime into a CLI that can run recipes by name, integrate with GitHub Actions, and work with Dependabot pull requests.

## How is allwrite different from OpenRewrite CLI?

OpenRewrite provides the transformation engine and a broad recipe ecosystem. allwrite builds an opinionated workflow on top of it: it adds human-friendly recipe names, bundles curated recipes, supports private recipes from external JARs, and integrates recipe execution with GitHub Actions and Dependabot. It is not a replacement for OpenRewrite; it makes selected OpenRewrite capabilities easier to discover and automate.

- **Group ID:** `pl.allegro.tech.allwrite`
- **License:** Apache 2.0
- **JVM toolchain:** Java 21

## Documentation

- [Overview](docs/index.md)
- [CLI reference](docs/cli.md)
- [Writing recipes](docs/contributing.md)
- [Built-in recipes](docs/recipes.md)

## Quick start

```bash
brew tap allegro/tap
brew install allwrite
allwrite ls
```

# allwrite
allwrite is a CLI tool and a collection of [OpenRewrite](https://docs.openrewrite.org/) recipes for automated code transformations across YAML, Gradle, Java/Kotlin, and Spring projects.

It wraps OpenRewrite's runtime into a CLI that can run recipes by name, integrate with GitHub Actions, and work with Dependabot pull requests.

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

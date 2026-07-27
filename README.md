# allwrite
What is `allwrite`? It's all you need for automatic code migrations!

- A CLI tool for running recipes via friendly names
- A collection of recipes filling the gaps in vanilla [OpenRewrite](https://docs.openrewrite.org)
- A GitHub Action integrating with Dependabot (coming soon)
- A GitHub bot automatically running migrations for Dependabot pull requests (coming soon)

## How is allwrite different from OpenRewrite CLI?

OpenRewrite provides the transformation engine and a broad recipe ecosystem. allwrite builds an opinionated workflow on top of it: it adds human-friendly recipe names, bundles curated recipes, supports private recipes from external JARs, and integrates recipe execution with GitHub Actions and Dependabot. It is not a replacement for OpenRewrite; it makes selected OpenRewrite capabilities easier to discover and automate.

## Documentation

- [Overview](docs/index.md)
- [CLI reference](docs/cli.md)
- [Writing recipes](docs/contributing.md)
- [Built-in recipes](docs/recipes/index.md)

## Quick start

```bash
brew tap allegro/tap
brew install allwrite
allwrite ls
```

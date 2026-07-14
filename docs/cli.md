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

Run a recipe by fully-qualified name:

```bash
allwrite run --recipe pl.allegro.tech.allwrite.recipes.SpringBoot4
```

Run recipes listed in a JSON file:

```bash
allwrite run --file recipes.json
```

The JSON object must contain a `recipes` array of fully-qualified recipe names.

The `run` command also supports:

| Option | Description |
|---|---|
| `--fail-on-error` | Stop execution when a recipe visitor reports an error. |
| `--continue-on-error` | Continue after recipe visitor errors. This is the default. |
| `-v`, `--verbose` | Enable verbose command output and debug logging. |
| `--log-level <level>` | Set the logging level explicitly. |

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

## Built-in recipes

The `allwrite` CLI comes with all the free OpenRewrite migrations bundled (Java/Kotlin refactoring, Spring Boot upgrades, etc.).

In addition, it provides a collection of custom recipes that aim to fill the gaps. See [the recipes reference](recipes.md) for the full list.

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

All external recipe subcommands also support `-v`/`--verbose` and `--log-level`.

## Dependabot automation

`allwrite run-dependabot` is an internal automation command used by the GitHub integration. It reads Dependabot metadata from `GH_BOT_EXTRA_PARAMS`, finds recipes tagged for the updated artifacts and version ranges, and executes the matching migrations.

| Option | Description |
|---|---|
| `--prm-extra <json>` | Dependabot pull-request metadata. It can also be supplied through the `GH_BOT_EXTRA_PARAMS` environment variable. |
| `--dump-execution-result <path>` | Write execution details to a JSON file. |

### Running from sources

To build and run from source without installing, use Gradle directly:

```bash
./gradlew :allwrite-cli:run --args "run springBoot/upgrade 3 4"
```

To run against a specific project directory:

```bash
./gradlew :allwrite-cli:run --args "run springBoot/upgrade 3 4" -Pworkdir=<path-to-projects>/some-project
```

Alternatively, build a local installation:

```bash
./gradlew :allwrite-cli:installDist
```

And run it as a regular binary:

```sh
<allwrite-root>/allwrite-cli/build/installation/bin/allwrite <args>
```

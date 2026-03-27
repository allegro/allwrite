# Custom Rules

- Never write any comments in code
- Update AGENTS.md every time project structure is changed

---

# Project Overview

**allwrite** is a CLI tool and collection of [OpenRewrite](https://docs.openrewrite.org/) recipes for automated code transformations across YAML, Gradle, Java/Kotlin, and Spring projects. It wraps OpenRewrite's runtime into a CLI that can run recipes by name, integrate with GitHub Actions, and work with Dependabot PRs.

- **Group ID:** `pl.allegro.tech.allwrite`
- **License:** Apache 2.0
- **JVM toolchain:** Java 21 (Temurin)

# Tech Stack

| Category | Technology |
|---|---|
| Primary language | Kotlin |
| Secondary language | Java (minimal, in `allwrite-recipes`) |
| Core framework | OpenRewrite |
| Dependency injection | Koin (with KSP annotation processing) |
| CLI framework | Clikt |
| Logging | SLF4J + Logback + kotlin-logging |
| Serialization | kotlinx-serialization |
| HTTP client | Ktor |
| Markdown rendering | Markout, clikt-markdown, Mordant |
| Build system | Gradle (Kotlin DSL) with composite build |
| Release management | JReleaser + Axion Release Plugin |
| Testing | Kotest (FunSpec), JUnit 5, MockK, OpenRewrite RewriteTest |

# Architecture

Multi-module Gradle project using **Hexagonal Architecture (Ports & Adapters)**.

## Module Dependency Graph

```
allwrite-cli  -->  allwrite-runtime  -->  allwrite-recipes
      |                                            ^
      +-----> allwrite-completions ----------------+
                    (annotation processor)
      |
      +-----> allwrite-recipes (direct, for recipe classpath)
```

## Module Responsibilities

| Module | Role |
|---|---|
| `allwrite-recipes` | Pure OpenRewrite recipe implementations. Published as a Maven artifact. |
| `allwrite-runtime` | Domain layer. Port interfaces and OpenRewrite-backed implementations. |
| `allwrite-cli` | Application + Infrastructure layer. CLI commands, OS/GitHub integration, DI wiring. |
| `allwrite-completions` | Build-time annotation processor for shell completion generation. |
| `build-logic` | Gradle composite build with convention plugins and custom tasks. |

# Directory Structure

```
allwrite/
├── allwrite-cli/
│   ├── src/main/kotlin/              CLI application (commands, infrastructure adapters)
│   │   ├── runner/Main.kt            Entry point
│   │   ├── runner/RunnerModule.kt
│   │   ├── runner/application/       CLI commands (run, ls, add, update, rm, refresh), application logic
│   │   ├── runner/infrastructure/    OS, GitHub, PR manager adapters (incl. external recipe store)
│   │   └── runner/util/              Utility classes
│   ├── src/main/resources/           logback.xml
│   ├── src/test/kotlin/              Unit tests (Kotest FunSpec)
│   └── src/e2e/kotlin/              End-to-end tests
│
├── allwrite-runtime/
│   ├── src/main/kotlin/              Ports, recipe executor, source file parser
│   ├── src/test/kotlin/              Unit tests (Kotest FunSpec)
│   └── src/testFixtures/kotlin/      Shared test fakes/fixtures
│
├── allwrite-recipes/
│   ├── src/main/kotlin/
│   │   ├── recipes/yaml/            YAML transformation recipes
│   │   ├── recipes/spring/          Spring property/annotation recipes
│   │   ├── recipes/java/            Java refactoring recipes
│   │   ├── recipes/gradle/          Gradle dependency recipes
│   │   ├── recipes/properties/      Properties file recipes
│   │   ├── recipes/toml/            TOML utilities
│   │   └── recipes/util/            Shared recipe utilities
│   ├── src/main/resources/META-INF/rewrite/   Declarative YAML recipes
│   ├── src/test/kotlin/              Unit tests (JUnit 5 + RewriteTest)
│   └── src/testFixtures/kotlin/      Test fixture classes
│
├── allwrite-completions/
│   └── src/main/kotlin/              kapt processors + generators
│
├── build-logic/
│   └── src/main/kotlin/
│       ├── conventions/              Convention plugins (kotlin, koin, jreleaser, etc.)
│       └── *.kt                      Custom Gradle tasks (FetchJdkTask, etc.)
│
├── gradle/
│   ├── libs.versions.toml            Central dependency version catalog
│   └── wrapper/
│
├── .github/workflows/               CI/CD
└── docs/                            Architecture diagrams
```

# Entry Point

`allwrite-cli/src/main/kotlin/pl/allegro/tech/allwrite/runner/Main.kt`

The `main()` function bootstraps Koin DI, conditionally loads `GithubModule` when running in GitHub Actions, then delegates to `AppEntrypoint.execute(args)`. `AppEntrypoint` is implemented by `MainCommand` (a Clikt command) dispatching to sub-commands.

# Coding Conventions

## Style

- **Kotlin explicit API mode** is enabled — all public declarations require explicit visibility modifiers
- `.editorconfig` enforces: 4-space indentation, 160 char line length, LF line endings, UTF-8
- Kotlin style: `ktlint_code_style = intellij_idea`, no trailing commas, no star imports
- YAML files: 2-space indentation
- Shell scripts: 2-space indentation

## Naming

- Kotlin files: PascalCase class names matching file names
- Test files: `*Spec.kt` (Kotest) or `*Test.kt` (JUnit)
- Packages: `pl.allegro.tech.allwrite.*`
- Koin modules: `*Module.kt`
- Port interfaces: under `port/incoming/` and `port/outgoing/`
- Fake test implementations: `Fake*` prefix

## Patterns

- **Hexagonal Architecture:** Incoming ports (`RecipeExecutor`, `RecipeSource`, `AppEntrypoint`) and outgoing ports (`UserProblemReporter`, `InputFilesProvider`, `GitMetadata`, `PullRequestContext`, `TelemetryPublisher`, `SystemInfo`, `ExternalRecipeJarsProvider`)
- **Koin + KSP DI:** Modules annotated with `@Module` and `@ComponentScan`, services with `@Single`
- **Convention Plugins:** Shared build logic in `build-logic/` (`conventions.kotlin`, `conventions.koin`, `conventions.recipe-classpaths`, etc.)
- **Template Method:** `SubCommand` sealed abstract class defines `run()` lifecycle; subclasses implement `runSubCommand()`
- **Observer/Listener:** `CommandListener` instances notified after each command execution (telemetry)
- **Recipe Strategy:** Recipes can implement `ParsingAwareRecipe` or `PostprocessingRecipe`. Base classes: `AllwriteRecipe`, `AllwriteScanningRecipe`
- **Test Fakes over Mocks:** Heavy use of `Fake*` implementations for test isolation
- **Tag-based Recipe Metadata:** Custom tag system (`visibility:public/internal`, `group:*`, `recipe:*`, `from:*`, `to:*`) for friendly names, version matching, visibility filtering
- **Declarative Recipes:** Recipes defined programmatically (Kotlin) or declaratively (YAML under `META-INF/rewrite/`)

# Testing

| Module | Location | Framework | Naming |
|---|---|---|---|
| `allwrite-cli` | `src/test/kotlin/` | Kotest FunSpec | `*Spec.kt` |
| `allwrite-cli` | `src/e2e/kotlin/` | Kotest FunSpec | `*IntegrationSpec.kt` |
| `allwrite-runtime` | `src/test/kotlin/` | Kotest FunSpec | `*Spec.kt` |
| `allwrite-recipes` | `src/test/kotlin/` | JUnit 5 + RewriteTest | `*Test.kt` |

- `BaseRunnerSpec` — abstract base for runner tests, sets up Koin DI with faked modules
- `BaseRuntimeSpec` — abstract base for runtime tests
- Test fixtures shared via `testFixtures` source set
- Recipe tests use OpenRewrite's `RewriteTest` interface for before/after source assertions

# Commands

| Command | Description |
|---|---|
| `./gradlew test` | Run unit tests across all modules |
| `./gradlew build` | Full build including JReleaser assembly |
| `./gradlew :allwrite-cli:run --args "<args>"` | Run the CLI locally |
| `./gradlew :allwrite-cli:e2e` | Run end-to-end tests |
| `./gradlew check` | Run tests + e2e |

# Key Dependencies

**Core:** openrewrite (rewrite-core, rewrite-java, rewrite-java-11, rewrite-java-17, rewrite-java-21, rewrite-kotlin, rewrite-yaml, rewrite-toml, rewrite-properties, rewrite-gradle, rewrite-groovy), openrewrite-recipe (rewrite-spring, rewrite-static-analysis), koin-core, koin-annotations, koin-ksp-compiler, clikt, clikt-markdown, markout, markout-markdown, kotlin-reflect, kotlinx-serialization-json, kotlinx-datetime

**Infrastructure:** ktor-client-core, ktor-client-cio, ktor-client-content-negotiation, ktor-serialization-kotlinx-json, github-client (Spotify), java-semver, snakeyaml, logback-classic, kotlin-logging, slf4j-api

**Build-only:** kotlinpoet-jvm, mustache-java, auto-service, commons-compress, pgpainless-core, jackson-module-kotlin

**Testing:** kotest-runner-junit5-jvm, kotest-assertions-core-jvm, kotest-framework-datatest, kotest-extensions-koin, mockk, rewrite-test, junit-jupiter, assertj-core

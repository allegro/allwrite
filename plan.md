# Multi-Pass Isolated Execution for ClasspathAwareRecipe

## Problem

`SpringBoot4_0` recipe combines OpenRewrite's built-in Spring Boot migration recipes with our custom recipes (`AddNonNullableTypeBoundsToSpringRepositories`,
`ReplaceStatusCodeValue`) via `getRecipeList()`.

All sub-recipes share a single parsing pass, meaning all `ClasspathAwareRecipe` classpaths get merged. Custom recipes declare stubs (e.g.,
`spring-data-commons-3`, `spring-data-jpa-3`, `spring-data-mongodb-4`) that can shadow/conflict with what OpenRewrite's migration recipes expect, causing
failures.

This is an **allwrite-specific problem** — in standard OpenRewrite (Maven/Gradle plugin), the parser uses the project's real compile classpath resolved by the
build tool. Individual recipes never declare classpath needs. The `ClasspathAwareRecipe` interface is an allwrite invention to work around not having the real
project classpath (using pre-packaged stub JARs instead).

## Considered Approaches

### A) SPI marker interface (`MultiPassRecipe`) detected by executor

The recipe explicitly declares isolated execution groups via a new SPI interface.

- ✅ Clean SPI contract, explicit intent
- ⚠️ Adds a new concept to SPI
- ⚠️ Tension with `getRecipeList()` — two composition mechanisms

### B) Automatic isolation: executor splits on `ClasspathAwareRecipe` boundaries ← CHOSEN

No new interface. The executor automatically groups sub-recipes and isolates `ClasspathAwareRecipe` instances into separate phases.

- ✅ Zero changes to recipe code
- ✅ No new SPI concepts
- ⚠️ Implicit — but well-logged and predictable

### C) Application-layer orchestration

The CLI command layer iterates and calls `execute()` per group.

- ✅ Executor stays simple
- ⚠️ Leaks execution concerns into CLI layer
- ⚠️ Every caller must know about multi-pass

**Decision:** Option B — automatic isolation in `OpenrewriteRecipeExecutor`.

## Design Decisions (Q&A)

### Should ClasspathAwareRecipe recipes with identical classpaths share a pass?

**No.** Always isolate for simplicity.

### Should order from `getRecipeList()` be preserved?

**Yes, strictly.** Split at each `ClasspathAwareRecipe` boundary:

- Given `[OR1, OR2, NonClassPathAware, AddNonNullable, OR3, OR4, NonClassPathAware2, ReplaceStatusCode]`
- Phases: `{OR1, OR2, NonClassPathAware}` → `{AddNonNullable}` → `{OR3, OR4, NonClassPathAware2}` → `{ReplaceStatusCode}`

### Should this apply to all recipes or only AllwriteRecipe?

**Any recipe whose `recipeList` contains `ClasspathAwareRecipe` (directly or nested).**

Initially we considered restricting to `AllwriteRecipe` only, but this breaks for **declarative YAML recipes**. OpenRewrite loads YAML recipes as
`DeclarativeRecipe` (extends `Recipe`, not `AllwriteRecipe`), so a type check on `AllwriteRecipe` would skip them entirely — even when their `recipeList`
includes children like `SpringBoot4_0` that contain `ClasspathAwareRecipe` sub-recipes.

Since `ClasspathAwareRecipe` is an allwrite-specific concept, any recipe (whether Kotlin class or declarative YAML) that transitively contains one needs
isolation. The executor should inspect `recipeList` of **any** `Recipe` — `getRecipeList()` is a standard, side-effect-free OpenRewrite API safe to call on
`DeclarativeRecipe`.

### Should we split recursively (nested composites)?

**Yes, unbounded recursion.** When a sub-recipe is a `Recipe` whose `recipeList` contains any `ClasspathAwareRecipe`, recursively expand its phases
into the parent's phase list.

**Rationale:** Clients may compose our recipes (e.g., `SpringBoot4_0`) inside their own declarative YAML recipe or external `AllwriteRecipe`. Without
recursive splitting, the nested `ClasspathAwareRecipe` sub-recipes would never get isolated, and classpath conflicts would re-emerge.

**Example:**

```
ClientMigration.recipeList = [SpringBoot4_0, SomeOtherRecipe]
  └─ SpringBoot4_0.recipeList = [OR_SpringUpgrade, AddNonNullable, ReplaceStatusCode]

Result phases:
  Phase 1: {OR_SpringUpgrade}        (from expanding SpringBoot4_0)
  Phase 2: {AddNonNullable}          (isolated, from SpringBoot4_0)
  Phase 3: {ReplaceStatusCode}       (isolated, from SpringBoot4_0)
  Phase 4: {SomeOtherRecipe}         (remaining top-level group)
```

**Detection:** A recipe "needs expansion" when `recipe.recipeList.any { it is ClasspathAwareRecipe || it.needsExpansion() }` (recursive check for deeply
nested cases). This applies to any `Recipe`, not just `AllwriteRecipe`.

### Input files — re-scan per pass or scan once?

**Scan once.** The `inputFiles: List<Path>` is provided once to `execute()`. Each phase re-parses from disk (picking up modifications from previous phases)
using the same file list, filtered by `Path::exists` to handle deletions.

Newly created files won't be picked up — acceptable since our recipes modify existing files.

### PostprocessingRecipe — per phase or at the end?

**Per phase.** Since later phases re-read from disk, post-processing must complete before the next phase starts.

### ParsingAwareRecipe — respected per phase?

**Yes.** Checked on the per-phase recipe(s).

### Error handling?

Same as today:

- `failOnError = true`: Phase throws → execution stops, remaining phases skipped. Changes from completed phases remain on disk.
- `failOnError = false` (default): Errors logged at DEBUG, all phases run regardless.

### Do we need a wrapper recipe for grouped phases?

**Yes**, but only as a **private/internal class in `allwrite-runtime`** (e.g., `internal class PhaseRecipe`). Not in SPI. It's purely an implementation detail —
needed to have a `Recipe` object to call `.run()` on and to scope `resolveClasspath`/`withExternalRecipeClassLoaders` to the phase's recipes.

### How do tests work?

- **Keep existing `SpringBoot4_0Test`** (uses `RewriteTest`) for recipe transformation correctness — runs with merged classpath via OpenRewrite's test
  infrastructure.
- **Add a new executor-level test** in `allwrite-runtime/src/test` to verify the splitting/isolation logic.

## Logging

```
Running recipe SpringBoot4_0                                            # INFO
Detected ClasspathAware recipes, splitting into 4 isolated phases       # INFO (only when splitting)
Phase 1: 3 non-classpath-aware recipes                                  # INFO
Grouped recipes: [OR1, OR2, NonClassPathAware]                          # DEBUG
Phase 2: AddNonNullableTypeBoundsToSpringRepositories                   # INFO
Phase 3: 3 non-classpath-aware recipes                                  # INFO
Grouped recipes: [OR3, OR4, NonClassPathAware2]                         # DEBUG
Phase 4: ReplaceStatusCodeValue                                         # INFO
```

When no `ClasspathAwareRecipe` sub-recipes exist — no splitting, existing behavior unchanged.

### Nested recipe expansion logging

When recursive expansion is triggered, log "Expanding nested recipe" at INFO level **at the point of encounter** (not all upfront):

```
Running recipe ClientMigration                                                    # INFO
Detected ClasspathAware recipes, splitting into 8 isolated phases                 # INFO
Expanding nested recipe SpringBoot4_0 (3 sub-phases)                              # INFO
Phase 1: 2 non-classpath-aware recipes                                            # INFO
Grouped recipes: [OR_UpgradeSpringBoot_4_0, OR_UpgradeSpringFramework_7_0]        # DEBUG
Run finished with 12 files modified and 0 files deleted                           # INFO
Phase 2: AddNonNullableTypeBounds                                                 # INFO
Run finished with 3 files modified and 0 files deleted                            # INFO
Phase 3: ReplaceStatusCodeValue                                                   # INFO
Run finished with 2 files modified and 0 files deleted                            # INFO
Phase 4: 1 non-classpath-aware recipes                                            # INFO
Grouped recipes: [RemoveDeprecatedEndpoints]                                      # DEBUG
Run finished with 1 files modified and 1 files deleted                            # INFO
Expanding nested recipe KotlinUpgrade (3 sub-phases)                              # INFO
Phase 5: 1 non-classpath-aware recipes                                            # INFO
Grouped recipes: [OR_UpgradeKotlin_2_2]                                           # DEBUG
Run finished with 8 files modified and 0 files deleted                            # INFO
Phase 6: FixSealedClassMigration                                                  # INFO
Run finished with 2 files modified and 0 files deleted                            # INFO
Phase 7: 1 non-classpath-aware recipes                                            # INFO
Grouped recipes: [OR_CleanupDeprecations]                                         # DEBUG
Run finished with 4 files modified and 0 files deleted                            # INFO
Phase 8: 1 non-classpath-aware recipes                                            # INFO
Grouped recipes: [UpdateGradleWrapper]                                            # DEBUG
Run finished with 1 files modified and 0 files deleted                            # INFO
```

## Implementation Plan

### Splitting Algorithm (in `OpenrewriteRecipeExecutor`)

1. Get `getRecipeList()` — check if any sub-recipe is `ClasspathAwareRecipe` or needs expansion (recursive check)
2. If none → single pass (existing behavior)
3. If any → walk list in order:
    - If sub-recipe is `ClasspathAwareRecipe` → flush accumulated group as a phase, emit it as its own phase
    - If sub-recipe needs expansion (contains `ClasspathAwareRecipe` at any depth):
      → flush accumulated group as a phase
      → log "Expanding nested recipe X (N sub-phases)" at INFO
      → recursively `splitIntoPhases(subRecipe)` and append each resulting phase
    - Otherwise → accumulate into current group
    - At end → flush remaining accumulated group
4. For each phase:
    - Filter `inputFiles` by `Path::exists`
    - Create phase recipe (single recipe or internal `PhaseRecipe` wrapper for groups)
    - `parseSourceFiles(phaseRecipe, inputFiles, context)`
    - `runRecipe(phaseRecipe, sourceFiles, context)`
    - `applyChanges(recipeRun)`
    - `postProcess(phaseRecipe)`

#### Helper: `needsExpansion()`

```kotlin
private fun Recipe.needsExpansion(): Boolean =
    recipeList.any { it is ClasspathAwareRecipe || it.needsExpansion() }
```

### Files to Modify

| File                                                              | Change                                                |
|-------------------------------------------------------------------|-------------------------------------------------------|
| `allwrite-runtime/.../OpenrewriteRecipeExecutor.kt`               | Add phase-splitting logic in `execute()`              |
| `allwrite-runtime/.../OpenrewriteRecipeExecutor.kt` (or new file) | Add internal `PhaseRecipe` class                      |
| `allwrite-runtime/src/test/kotlin/...`                            | Add executor-level test for isolation behavior        |
|                                                                   | Including: nested `AllwriteRecipe` expansion scenario |

### Files NOT Modified

| File                                    | Reason                       |
|-----------------------------------------|------------------------------|
| `allwrite-spi/`                         | No new interfaces or classes |
| `allwrite-api/RecipeExecutor.kt`        | Interface unchanged          |
| `allwrite-recipes/.../SpringBoot4_0.kt` | No changes needed            |
| `allwrite-cli/`                         | No changes needed            |

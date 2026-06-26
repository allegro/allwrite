# Plan: Replace `statusCodeValue` in Groovy/Kotlin files

## Problem

`ResponseEntity.getStatusCodeValue()` (Java) / `.statusCodeValue` (Groovy property access) was removed in Spring Framework 7 / Spring Boot 4. The replacement is
`.getStatusCode().value()` (Java) / `.statusCode.value()` (Groovy).

OpenRewrite's `UpgradeSpringBoot_4_0` (via `rewrite-spring` 6.30.3) handles this for Java. We need to verify whether it also covers Groovy and Kotlin files. If
not, we implement a custom recipe.

## Phase 1: Spike Test (Verification)

**Goal:** Determine if the existing `UpgradeSpringBoot_4_0` recipe (from `rewrite-spring`) already handles `statusCodeValue` replacement in Groovy and Kotlin
files.

**File:** `allwrite-recipes/src/test/kotlin/pl/allegro/tech/allwrite/recipes/spring/ReplaceStatusCodeValueSpikeTest.kt`

**Approach:**

1. Write a `RewriteTest` that loads the `UpgradeSpringBoot_4_0` recipe (or the specific sub-recipe that handles `statusCodeValue` in Java)
2. Provide Groovy source specs using `groovy()` helper with:
    - `.statusCodeValue` property access → expected: `.statusCode.value()`
    - `.getStatusCodeValue()` method call → expected: `.getStatusCode().value()`
3. Provide Kotlin source specs using `kotlin()` helper with:
    - `.statusCodeValue` property access → expected: `.statusCode.value()`
    - `.getStatusCodeValue()` method call → expected: `.getStatusCode().value()`
4. Run the spike test with `./gradlew :allwrite-recipes:test --tests "*SpikeTest*"`
5. Observe results:
    - If tests **pass** → recipe already covers these languages, no further work needed
    - If tests **fail** (no transformation applied) → proceed to Phase 2

**Pattern reference:** See existing tests in `DeleteSpringPropertyFromSpringAnnotationsTest.kt` and `RenameTaskExecutorBeanTest.kt` for how `groovy()` and
`kotlin()` source specs are used with `RewriteTest`.

## Phase 2: Write Real Tests (if spike fails)

**File:** `allwrite-recipes/src/test/kotlin/pl/allegro/tech/allwrite/recipes/spring/ReplaceStatusCodeValueTest.kt`

**Test cases (Groovy):**

- `.statusCodeValue` property access → `.statusCode.value()`
- `.getStatusCodeValue()` method call → `.getStatusCode().value()`
- No change when `statusCodeValue` is not from `ResponseEntity`
- Handles chained calls (e.g., `response.statusCodeValue == 200`)

**Test cases (Kotlin):**

- `.statusCodeValue` property access → `.statusCode.value()`
- `.getStatusCodeValue()` method call → `.getStatusCode().value()`
- No change when `statusCodeValue` is not from `ResponseEntity`

**Test structure:** JUnit 5 + `RewriteTest` interface (standard for `allwrite-recipes` module)

## Phase 3: Implementation (if spike fails)

**File:** `allwrite-recipes/src/main/kotlin/pl/allegro/tech/allwrite/recipes/spring/ReplaceStatusCodeValue.kt`

**Possible approaches (decide based on spike findings):**

### Option A: OpenRewrite Groovy/Kotlin Visitor

- Use `GroovyVisitor` / `KotlinVisitor` to find method invocations / property accesses
- Replace AST nodes for `getStatusCodeValue()` → `getStatusCode().value()`
- Replace property access `statusCodeValue` → `statusCode.value()`
- More precise, type-aware, but more complex

### Option B: Text-based `FindAndReplace`

- Use `org.openrewrite.text.FindAndReplace` with regex
- Pattern: `\.statusCodeValue\b` → `.statusCode.value()`
- Pattern: `\.getStatusCodeValue\(\)` → `.getStatusCode().value()`
- `filePattern = "**/*.groovy"` and `"**/*.kt"`
- Simpler, less precise (no type checking), but effective for common patterns
- Project already has a `FindAndReplace` wrapper in `recipes/util/`

### Option C: Composite recipe (declarative YAML)

- Define in `META-INF/rewrite/` as a declarative recipe combining text-based replacements
- Good if the fix is purely textual

**Decision criteria:** If type-awareness is needed (to avoid false positives), go with Option A. If the pattern is distinctive enough (`.statusCodeValue` is
unique to Spring's `ResponseEntity`), Option B or C is simpler and sufficient.

## Phase 4: Integration into SpringBoot4_0

- If a new recipe is created, add it to the recipe list returned by `SpringBoot4_0` (or its underlying `UpgradeSpringBoot_4_0` recipe chain)
- Alternatively, if this is a standalone recipe, register it in `META-INF/rewrite/` and document it

## Phase 5: Documentation & Cleanup

- Remove spike test file
- Update `RECIPES.md` with the new recipe documentation (if a new recipe was created)
- Run full test suite: `./gradlew :allwrite-recipes:test`

## Key References

| File                                                                                           | Purpose                                        |
|------------------------------------------------------------------------------------------------|------------------------------------------------|
| `allwrite-recipes/src/test/kotlin/.../RewriteAssertionKotlinWrappers.kt`                       | `groovy()`, `kotlin()` test helpers            |
| `allwrite-recipes/src/test/kotlin/.../spring/DeleteSpringPropertyFromSpringAnnotationsTest.kt` | Example of Groovy test specs                   |
| `allwrite-recipes/src/test/kotlin/.../spring/RenameTaskExecutorBeanTest.kt`                    | Example of Kotlin + Java test specs            |
| `allwrite-recipes/src/main/kotlin/.../spring/IsolatedSpringRecipe.kt`                          | How Spring Boot upgrade recipes are structured |
| `allwrite-recipes/src/main/kotlin/.../spring/SpringBoot4_0.kt`                                 | The Spring Boot 4.0 upgrade recipe             |
| `allwrite-recipes/src/main/kotlin/.../util/FindAndReplace.kt`                                  | Text-based find/replace wrapper                |

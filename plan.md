# Plan: Replace `statusCodeValue` in Groovy/Kotlin/Java files

## Problem

`ResponseEntity.getStatusCodeValue()` (Java) / `.statusCodeValue` (Groovy/Kotlin property access) was removed in Spring Framework 7 / Spring Boot 4. The
replacement is
`.getStatusCode().value()` (Java) / `.statusCode.value()` (Groovy/Kotlin).

## Phase 1: Spike Test (Verification) ✅ DONE

**Goal:** Determine if the existing `UpgradeSpringBoot_4_0` recipe (from `rewrite-spring`) already handles `statusCodeValue` replacement.

**File:** `allwrite-recipes/src/test/kotlin/pl/allegro/tech/allwrite/recipes/spring/ReplaceStatusCodeValueSpikeTest.kt`

**Result:** The recipe does NOT handle this transformation for ANY language (Java, Groovy, or Kotlin). All tests failed with "Recipe was expected to make a
change but made no changes."

**Additional findings:**

- `rewrite-spring` 6.30.3 contains NO recipe for `ResponseEntity.getStatusCodeValue()` / `.statusCodeValue`
- The only related recipe is `MigrateResponseStatusExceptionGetRawStatusCodeMethod` (Spring Framework 6.0 migration) which handles
  `ResponseStatusException.getRawStatusCode()` → `getStatusCode().value()` — a **different class**
- Web search confirms no such recipe exists in the broader OpenRewrite ecosystem either
- The custom recipe must cover **all three languages**: Java, Groovy, and Kotlin

## Phase 2: Write Real Tests (spike confirmed gap) ✅ DONE

**File:** `allwrite-recipes/src/test/kotlin/pl/allegro/tech/allwrite/recipes/spring/ReplaceStatusCodeValueTest.kt`

**Minimal recipe stub (for compilation):** `allwrite-recipes/src/main/kotlin/pl/allegro/tech/allwrite/recipes/spring/ReplaceStatusCodeValue.kt`

**Test cases (Java):**

- `.getStatusCodeValue()` method call → `.getStatusCode().value()`
- No change when `getStatusCodeValue()` is not from `ResponseEntity`
- Handles chained calls (e.g., `response.getStatusCodeValue() == 200`)

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

## Phase 3: Implementation ✅ DONE

**File:** `allwrite-recipes/src/main/kotlin/pl/allegro/tech/allwrite/recipes/spring/ReplaceStatusCodeValue.kt`

**Approach chosen: Option A — Type-aware `JavaVisitor` with `JavaTemplate` + `DelegatingJVisitor`**

Modelled after `rewrite-spring`'s `MigrateResponseStatusExceptionGetRawStatusCodeMethod`, which uses the same
`#{any()}.getStatusCode().value()` template pattern.

**Implementation details:**

- `MethodMatcher("org.springframework.http.ResponseEntity getStatusCodeValue()")` for Java method calls
- Fallback matching via `TypeUtils.isAssignableTo` on the select expression type (handles Groovy/Kotlin cases where
  `MethodMatcher` doesn't fully resolve types)
- `KotlinPropertyMatcher` + `TypeUtils.isAssignableTo` for `J.FieldAccess` nodes (Kotlin/Groovy `.statusCodeValue` property access)
- `JavaTemplate.builder("#{any()}.getStatusCode().value()")` with `classpathFromResources(ctx, "spring-web-6", "spring-core-6")`
  applied at both `J.MethodInvocation.coordinates.replace()` and `J.FieldAccess.coordinates.replace()`
- `DelegatingJVisitor(javaVisitor)` dispatches the single `JavaVisitor` across Java, Kotlin, and Groovy compilation units
- Reused existing project infrastructure: `KotlinPropertyMatcher`, `DelegatingJVisitor`, `ClasspathAwareRecipe`

**Output behavior:**

- Property access (`.statusCodeValue`) in Groovy/Kotlin is replaced with idiomatic `.statusCode.value()` — achieved by
  renaming the `J.FieldAccess` node from `statusCodeValue` to `statusCode` (preserving property-style rendering by
  Groovy/Kotlin printers) and wrapping with `#{any(org.springframework.http.HttpStatusCode)}.value()` template.
- Explicit method calls (`.getStatusCodeValue()`) in all languages are replaced with `.getStatusCode().value()` via
  `JavaTemplate("#{any()}.getStatusCode().value()")`.

**All 15 tests pass.** (The spike test `ReplaceStatusCodeValueSpikeTest` still fails as expected — it tests the upstream
`UpgradeSpringBoot_4_0` recipe which doesn't handle this case.)

## Phase 4: Integration into SpringBoot4_0 ✅ DONE

- Overrode `getRecipeList()` in `SpringBoot4_0` to append `ReplaceStatusCodeValue()` to the upstream recipe list
- Updated spike test (`ReplaceStatusCodeValueSpikeTest`) to verify integration:
  - Starts Koin with a `FakeRecipeSource` providing the upstream `UpgradeSpringBoot_4_0` recipe
  - Asserts `SpringBoot4_0().recipeList` contains `ReplaceStatusCodeValue`
  - Runs `ReplaceStatusCodeValue` (extracted from `SpringBoot4_0`) through `RewriteTest` for all 3 languages
  - All 6 tests pass

## Phase 5: Documentation & Cleanup ✅ DONE

- Spike test repurposed as integration test (verifies correct wiring into `SpringBoot4_0`)
- Full test suite passes: `./gradlew :allwrite-recipes:test` — 383 tests, 0 failures

## Key References

| File                                                                                           | Purpose                                        |
|------------------------------------------------------------------------------------------------|------------------------------------------------|
| `allwrite-recipes/src/test/kotlin/.../RewriteAssertionKotlinWrappers.kt`                       | `groovy()`, `kotlin()` test helpers            |
| `allwrite-recipes/src/test/kotlin/.../spring/DeleteSpringPropertyFromSpringAnnotationsTest.kt` | Example of Groovy test specs                   |
| `allwrite-recipes/src/test/kotlin/.../spring/RenameTaskExecutorBeanTest.kt`                    | Example of Kotlin + Java test specs            |
| `allwrite-recipes/src/main/kotlin/.../spring/IsolatedSpringRecipe.kt`                          | How Spring Boot upgrade recipes are structured |
| `allwrite-recipes/src/main/kotlin/.../spring/SpringBoot4_0.kt`                                 | The Spring Boot 4.0 upgrade recipe             |
| `allwrite-recipes/src/main/kotlin/.../util/FindAndReplace.kt`                                  | Text-based find/replace wrapper                |

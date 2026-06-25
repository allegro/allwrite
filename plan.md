# Plan: Add Non-Nullable Type Bounds to Spring Data Repository Type Parameters

## Problem

Spring Boot 4 / Spring Framework 7 adds JSpecify nullability annotations to Spring Data interfaces. In Kotlin, this
means type parameters on repository interfaces (`MongoRepository<T, ID>`, `CrudRepository<T, ID>`, etc.) now require
`T : Any` and `ID : Any` bounds.

## Scope (MVP)

Focus only on **direct repository interface extensions** — find interfaces/classes extending Spring Data repository
interfaces and add `: Any` bounds to their type parameters if missing.

**Out of scope (for now):** method signatures where generic types are passed to Spring APIs requiring `: Any` bounds.

## Approach

This is a Kotlin-specific recipe that uses OpenRewrite's Kotlin AST (`rewrite-kotlin`) to:

1. Visit class/interface declarations
2. Check if they extend a known Spring Data repository interface
3. For each type parameter passed to the repository supertype, ensure the corresponding type parameter on the declaring
   class has an `: Any` bound

## Implementation Steps

### 0. Spike: Validate Kotlin AST feasibility (BEFORE writing the real recipe) — ✅ DONE

**Outcome: visitor-based approach is FEASIBLE.** Validated via throwaway test
`allwrite-recipes/src/test/kotlin/pl/allegro/tech/allwrite/recipes/spring/AddNonNullableTypeBoundsSpikeTest.kt`
(4 cases, all passing). Findings:

1. **Bounds in the AST:**
    - `interface Repo<T, ID>` → each `J.TypeParameter` has `bounds = null`.
    - `interface Repo<T : Any, ID : Any>` → each `J.TypeParameter` has `bounds = [J.Identifier("Any")]` with type
      `kotlin.Any`.
2. **Matching the repository supertype:** the supertype is in `classDecl.implements` as a `J.ParameterizedType`; its
   `.clazz` is a `J.FieldAccess` with `.simpleName == "CrudRepository"` (FQN target
   `org.springframework.data.repository`). Matching works even without the classpath.
3. **CRITICAL printing gotcha:** adding bounds via `tp.withBounds(...)` persists across cycles, **but** the Kotlin
   printer (`KotlinPrinter.visitTypeParameter`) only emits the `:` separator when the `J.TypeParameter` carries a
   `org.openrewrite.kotlin.marker.TypeReferencePrefix` marker. Without it the output is invalid Kotlin (`T Any`). Fix:
   set bounds + the bounds `JContainer.before` space + add the `TypeReferencePrefix` marker → produces `T : Any`.
   Working helper from the spike:
   ```kotlin
   private fun addAnyBound(tp: J.TypeParameter): J.TypeParameter {
       val anyIdentifier = J.Identifier(UUID.randomUUID(), Space.format(" "), Markers.EMPTY, emptyList(),
           "Any", JavaType.buildType("kotlin.Any"), null)
       var newTp = tp.withBounds(listOf<TypeTree>(anyIdentifier))
       newTp = newTp.padding.withBounds(newTp.padding.bounds!!.withBefore(Space.format(" ")))
       newTp = newTp.withMarkers(newTp.markers.add(TypeReferencePrefix(UUID.randomUUID(), Space.EMPTY)))
       return newTp
   }
   ```
4. **Change detection:** compare per-element (track a `changed` flag) rather than `newList != oldList`, because
   `J.TypeParameter` equality is id-based and a mutated copy keeps the same id.
5. **Test type validation:** without Spring Data on the parser classpath, unresolved `T`/`ID` fail OpenRewrite type
   validation. The spike used `TypeValidation.none()`; the real recipe should instead use `ClasspathAwareRecipe` +
   `.withRecipeClasspath()` for robust FQN matching (see Step 2 / References).

**Status of the throwaway spike test:** kept on disk pending review; delete (or fold useful cases into the real test)
before finishing.

### 1. Write the test first (`AddNonNullableTypeBoundsToSpringRepositoriesTest.kt`) — ✅ DONE

- **Location:**
  `allwrite-recipes/src/test/kotlin/pl/allegro/tech/allwrite/recipes/spring/AddNonNullableTypeBoundsToSpringRepositoriesTest.kt`
- **Framework:** JUnit 5 + OpenRewrite `RewriteTest`
- **Stub recipe created:** `AddNonNullableTypeBoundsToSpringRepositories.kt` (compiles, no-op visitor)
- **Parser classpath:** added `org.springframework.data:spring-data-commons:3.5.+` to
  `allwrite-recipes/build.gradle.kts`
- **Test results (TDD red):** 13 tests total — 10 fail (expecting transformations), 3 pass (no-change cases)
- **Test cases implemented:**
    - Basic case: `interface Repo<T, ID> : CrudRepository<T, ID>` → adds `: Any` to both
    - Already bounded: `interface Repo<T : Any, ID : Any> : CrudRepository<T, ID>` → no change
    - Partial: one param already has `: Any`, the other doesn't
    - Different repository types: `Repository`, `ListCrudRepository`, `PagingAndSortingRepository`,
      `ListPagingAndSortingRepository`, `ReactiveCrudRepository`, `ReactiveSortingRepository`
    - Multiple type params with extra non-repo params (e.g. `<T, ID, Extra>` where only T and ID need bounds)
    - Class (not interface) extending a repository
    - Existing bound other than `Any` (e.g. `T : Serializable`) — should NOT add `: Any`
    - Non-repository interface — no change
    - Java repository interface — no change (Kotlin-only recipe)
    - Groovy repository interface — no change (Kotlin-only recipe)

### 2. Implement the recipe (`AddNonNullableTypeBoundsToSpringRepositories.kt`) — ✅ DONE

- **Location:**
  `allwrite-recipes/src/main/kotlin/pl/allegro/tech/allwrite/recipes/spring/AddNonNullableTypeBoundsToSpringRepositories.kt`
- **Base class:** `AllwriteRecipe` (non-scanning, single-pass recipe) + `ClasspathAwareRecipe`
- **Visitor:** `JavaIsoVisitor<ExecutionContext>` (works on Kotlin AST via shared `J` tree nodes)
- **Algorithm (implemented):**
    1. Override `visitClassDeclaration`
    2. Check if any supertype (`classDecl.implements` or `classDecl.extends`) is a known Spring Data repository FQN
       (using `TypeUtils.isAssignableTo` for robust FQN matching)
    3. For each matching supertype, identify which type parameters are passed to it (by inspecting
       `J.ParameterizedType.typeParameters` — the type arguments)
    4. Map those back to the declaring class's type parameter list by name
    5. For any matched type parameter that has no existing upper bound, add `: Any` bound
- **FQN matching:** Uses `TypeUtils.isAssignableTo` with the full list of 10 Spring Data repository FQNs
- **Visibility:** `INTERNAL`
- **Classpath:** `spring-data-commons-3` (added `org.springframework.data:spring-data-commons:3.5.+` to
  `allwrite-recipes/build.gradle.kts` `recipeDependencies`)
- **Kotlin-only guard:** Overrides `visitCompilationUnit` with `if (!cursor.isKotlin()) return cu` using the existing
  `Cursor.isKotlin()` extension from `recipes/util/Cursor.kt` (which checks `firstEnclosing(K::class.java) != null`).
  The `!is K.CompilationUnit` approach broke with Kotlin 2.4 ("Check for instance is always true" error).
- **Test results:** All 15 tests pass (13 Kotlin + 2 non-Kotlin no-change)

### 3. Wire into Spring Boot 4.0 recipe list — ✅ DONE

- Overrode `getRecipeList()` in `SpringBoot4_0` to append `AddNonNullableTypeBoundsToSpringRepositories()` to the
  parent's recipe list (`super.getRecipeList() + AddNonNullableTypeBoundsToSpringRepositories()`)
- This means the recipe runs as part of the Spring Boot 4.0 migration, after OpenRewrite's built-in recipes
- Compilation and all 15 recipe tests pass

### 4. Run tests and verify — ✅ DONE

- Full `./gradlew :allwrite-recipes:test` suite passes (including all 15 `AddNonNullableTypeBoundsToSpringRepositories` tests)

### 5. Update RECIPES.md — ✅ DONE

- Added documentation entry in the Spring section of `RECIPES.md` with before/after Kotlin examples

## Key References

- **Test pattern:** `RenameTaskExecutorBeanTest.kt` — uses `kotlin(before, after)` helper for Kotlin source assertions
- **Recipe pattern:** `RenameTaskExecutorBean.kt` — `AllwriteScanningRecipe` with `ClasspathAwareRecipe`
- **Kotlin test helpers:** `RewriteAssertionKotlinWrappers.kt` — `kotlin(before, after)` function
- **Classpath for tests:** `withRecipeClasspath()` loads JARs onto parser classpath for type resolution
- **AST shared types:** `J.ClassDeclaration`, `J.TypeParameter` — shared between Java and Kotlin parsers
- **Kotlin-specific visitor:** Can use `JavaIsoVisitor` which works on Kotlin AST via the shared `J` nodes

## Open Questions / Risks

1. ~~**Kotlin AST representation of type bounds:**~~ ✅ RESOLVED (spike):
   `J.TypeParameter.bounds = [J.Identifier("Any")]`
   with type `kotlin.Any`; `bounds = null` when unbounded.
2. ~~**Type resolution without classpath:**~~ ✅ RESOLVED: The recipe implements `ClasspathAwareRecipe` with
   `requireOnClasspath() = listOf("spring-data-commons-3")` and uses `TypeUtils.isAssignableTo` for robust FQN matching.
3. ~~**Printing modified Kotlin AST:**~~ ✅ RESOLVED (spike): the `KotlinTemplate` limitation does not apply here —
   direct
   AST mutation of `J.TypeParameter` prints correctly **provided** the `TypeReferencePrefix` marker is added (see Step
   0).
4. ~~**Type-param ↔ supertype mapping:**~~ ✅ RESOLVED: `findTypeParamsPassedToRepository()` collects only type param
   names actually passed to the repository supertype. Only those are bounded — extra params (e.g. `Extra` in
   `<T, ID, Extra>`) are left untouched.

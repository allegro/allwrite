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

### 2. Implement the recipe (`AddNonNullableTypeBoundsToSpringRepositories.kt`)

- **Location:**
  `allwrite-recipes/src/main/kotlin/pl/allegro/tech/allwrite/recipes/spring/AddNonNullableTypeBoundsToSpringRepositories.kt`
- **Base class:** `AllwriteRecipe` (non-scanning, single-pass recipe)
- **Visitor:** `JavaIsoVisitor<ExecutionContext>` (works on Kotlin AST via shared `J` tree nodes)
- **Algorithm:**
    1. Override `visitClassDeclaration`
    2. Check if any supertype (`classDecl.implements` or `classDecl.extends`) is a known Spring Data repository FQN
    3. For each matching supertype, identify which type parameters are passed to it
    4. Map those back to the declaring class's type parameter list
    5. For any type parameter that has no existing upper bound, add `: Any` bound
- **Known Spring Data repository interfaces to match:**
    - `org.springframework.data.repository.Repository`
    - `org.springframework.data.repository.CrudRepository`
    - `org.springframework.data.repository.ListCrudRepository`
    - `org.springframework.data.repository.PagingAndSortingRepository`
    - `org.springframework.data.repository.ListPagingAndSortingRepository`
    - `org.springframework.data.jpa.repository.JpaRepository`
    - `org.springframework.data.mongodb.repository.MongoRepository`
    - `org.springframework.data.mongodb.repository.ReactiveMongoRepository`
    - `org.springframework.data.repository.reactive.ReactiveCrudRepository`
    - `org.springframework.data.repository.reactive.ReactiveSortingRepository`
- **Visibility:** `INTERNAL` (part of Spring Boot 4.0 migration)
- **Key challenge — RESOLVED by spike:** Modifying `J.TypeParameter` to add a `: Any` bound requires (a) setting
  `bounds = [J.Identifier("Any")]` (type `kotlin.Any`), (b) setting the bounds `JContainer.before` space, and (c) adding
  an `org.openrewrite.kotlin.marker.TypeReferencePrefix` marker so the Kotlin printer emits the `:`. See Step 0 for the
  exact `addAnyBound()` helper. Use a `changed` flag for change detection (id-based equality).
- **Note (out of MVP scope):** the spike matched only `CrudRepository` by `J.FieldAccess.simpleName`. The real recipe
  must match the full list above; with `ClasspathAwareRecipe` + classpath, prefer FQN/`TypeUtils` matching on the
  supertype rather than simple-name string comparison.

### 3. Wire into Spring Boot 4.0 recipe list

- The `SpringBoot4_0` class is an `IsolatedSpringRecipe` that delegates to OpenRewrite's built-in migration.
- Our custom recipe should be added to the Spring Boot 4.0 migration. Check how other recipes are composed and register
  this one accordingly.
- Alternatively, it may need to be a standalone recipe if it doesn't fit the `IsolatedSpringRecipe` pattern.

### 4. Run tests and verify

- `./gradlew :allwrite-recipes:test --tests "*AddNonNullableTypeBoundsToSpringRepositories*"`
- Ensure the Kotlin parser correctly handles the before/after transformations

### 5. Update RECIPES.md

- Add documentation entry for the new recipe in the Spring section

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
2. **Type resolution without classpath:** The recipe might not need Spring Data on the classpath if it only inspects
   supertype names (text-based). But for robust FQN matching, classpath is needed → implement `ClasspathAwareRecipe`.
   (Spike confirmed simple-name matching works classpath-free, but FQN matching is preferred for the full repo list.)
3. ~~**Printing modified Kotlin AST:**~~ ✅ RESOLVED (spike): the `KotlinTemplate` limitation does not apply here —
   direct
   AST mutation of `J.TypeParameter` prints correctly **provided** the `TypeReferencePrefix` marker is added (see Step
   0).
4. **Type-param ↔ supertype mapping (still open):** the MVP currently bounds *every* unbounded type param on a matching
   class. The plan's algorithm (Step 2) intends to bound only params actually passed to the repository supertype. Decide
   in Step 1/2 whether to scope strictly to those params or bound all (e.g. for `<T, ID, Extra>`).

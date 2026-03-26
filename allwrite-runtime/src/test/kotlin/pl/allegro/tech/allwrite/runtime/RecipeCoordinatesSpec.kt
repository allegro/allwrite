package pl.allegro.tech.allwrite.runtime

import com.github.zafarkhaja.semver.Version
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.api.RecipeCoordinates
import pl.allegro.tech.allwrite.api.getFromVersion
import pl.allegro.tech.allwrite.api.getToVersion
import pl.allegro.tech.allwrite.api.toRecipeCoordinatesOrNull
import java.net.URI

class RecipeCoordinatesSpec : FunSpec() {
    init {
        test("should return group and name when versions are not present") {
            RecipeCoordinates(
                group = "test",
                action = "example",
                fromVersion = null,
                toVersion = null
            ).toString() shouldBeEqual "test/example"
        }

        test("should not return toVersion when fromVersion is null") {
            RecipeCoordinates(
                group = "test",
                action = "example",
                fromVersion = null,
                toVersion = Version.of(1L)
            ).toString() shouldBeEqual "test/example"
        }

        test("should return fromVersion when it is not null") {
            RecipeCoordinates(
                group = "test",
                action = "example",
                fromVersion = Version.of(1L, 2L),
                toVersion = null
            ).toString() shouldBeEqual "test/example 1.2"
        }

        test("should return fromVersion and toVersion when they are not null") {
            RecipeCoordinates(
                group = "test",
                action = "example",
                fromVersion = Version.of(1L, 2L),
                toVersion = Version.of(3L, 4L, 5L)
            ).toString() shouldBeEqual "test/example 1.2 3.4.5"
        }

        test("should map descriptor to coordinates") {
            // given
            val recipeDescriptor = RecipeDescriptor(
                tags = setOf(
                    "group:some-group",
                    "action:some-action",
                    "from:1.0.0",
                    "to:2.0.0",
                )
            )

            // when
            val recipeCoordinates = recipeDescriptor.toRecipeCoordinatesOrNull()

            // then
            recipeCoordinates shouldBe RecipeCoordinates(
                group = "some-group",
                action = "some-action",
                fromVersion = Version.parse("1.0.0", false),
                toVersion = Version.parse("2.0.0", false)
            )
        }

        test("should map descriptor to coordinates with null versions") {
            // given
            val recipeDescriptor = RecipeDescriptor(
                tags = setOf(
                    "group:some-group",
                    "action:some-action",
                )
            )

            // when
            val recipeCoordinates = recipeDescriptor.toRecipeCoordinatesOrNull()

            // then
            recipeCoordinates shouldBe RecipeCoordinates(
                group = "some-group",
                action = "some-action",
                fromVersion = null,
                toVersion = null
            )
        }

        test("should not map descriptor to coordinates with null recipe") {
            // given
            val recipeDescriptor = RecipeDescriptor(
                tags = setOf(
                    "group:some-group",
                )
            )

            // when
            val recipeCoordinates = recipeDescriptor.toRecipeCoordinatesOrNull()

            // then
            recipeCoordinates shouldBe null
        }

        test("should not map descriptor to coordinates with null group") {
            // given
            val recipeDescriptor = RecipeDescriptor(
                tags = setOf(
                    "action:some-action",
                )
            )

            // when
            val recipeCoordinates = recipeDescriptor.toRecipeCoordinatesOrNull()

            // then
            recipeCoordinates shouldBe null
        }

        test("should get fromVersion from descriptor") {
            // given
            val recipeDescriptor = RecipeDescriptor(
                tags = setOf(
                    "from:1.0.0",
                )
            )

            // when
            val fromVersion = recipeDescriptor.getFromVersion()

            // then
            fromVersion shouldBe Version.parse("1.0.0", false)
        }

        test("should get toVersion from descriptor") {
            // given
            val recipeDescriptor = RecipeDescriptor(
                tags = setOf(
                    "to:2.0.0",
                )
            )

            // when
            val toVersion = recipeDescriptor.getToVersion()

            // then
            toVersion shouldBe Version.parse("2.0.0", false)
        }
    }
}

@Suppress("FunctionNaming")
private fun RecipeDescriptor(tags: Set<String>) =
    RecipeDescriptor(
        "name",
        "display name",
        "instance name",
        "description",
        tags,
        null,
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        URI("file:///not-used")
    )

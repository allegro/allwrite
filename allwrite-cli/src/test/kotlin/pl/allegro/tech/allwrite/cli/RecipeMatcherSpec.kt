package pl.allegro.tech.allwrite.cli

import com.github.zafarkhaja.semver.Version
import io.kotest.matchers.equals.shouldBeEqual
import pl.allegro.tech.allwrite.api.RecipeCoordinates
import pl.allegro.tech.allwrite.cli.application.RecipeMatcher
import pl.allegro.tech.allwrite.cli.base.BaseCliSpec
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipe
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipeSource
import pl.allegro.tech.allwrite.runtime.util.injectEagerly

class RecipeMatcherSpec : BaseCliSpec() {

    override fun additionalModules() =
        listOf(
            TestModules.fakeRuntime,
        )

    private val recipeMatcher: RecipeMatcher by injectEagerly()

    init {
        test("should return exact match when present") {
            // when
            val recipeDescriptors = recipeMatcher.findMatching(
                RecipeCoordinates("spring-boot", "upgrade", Version.of(3L), Version.of(4L)),
            )

            // then
            recipeDescriptors shouldBeEqual listOf(FakeRecipeSource.Companion.SPRING_BOOT_4_TEST_RECIPE.descriptor)
        }

        test("should return all matching when no exact match and version not specified") {
            // when
            val recipeDescriptors = recipeMatcher.findMatching(
                RecipeCoordinates("spring-boot", "upgrade", null, null),
            )

            // then
            recipeDescriptors shouldBeEqual
                listOf(FakeRecipeSource.Companion.SPRING_BOOT_3_TEST_RECIPE.descriptor, FakeRecipeSource.Companion.SPRING_BOOT_4_TEST_RECIPE.descriptor)
        }

        test("should return empty list when no exact match and versions are specified") {
            // when
            val recipeDescriptors = recipeMatcher.findMatching(
                RecipeCoordinates("spring-boot", "upgrade", Version.of(777L), Version.of(999L)),
            )

            // then
            recipeDescriptors shouldBeEqual emptyList()
        }

        test("should return chain of recipes when only target version is provided") {
            // when
            val recipeDescriptors = recipeMatcher.findMatchingByTargetVersion(
                "spring-boot",
                "upgrade",
                Version.of(4L),
            )

            // then
            recipeDescriptors shouldBeEqual listOf(
                FakeRecipeSource.Companion.SPRING_BOOT_3_TEST_RECIPE.descriptor,
                FakeRecipeSource.Companion.SPRING_BOOT_4_TEST_RECIPE.descriptor,
            )
        }

        test("should return direct recipe when it covers the full range") {
            // given
            val directRecipe = FakeRecipe(
                id = "pl.allegro.tech.allwrite.recipes.spring-boot-2-to-4",
                tags = setOf("visibility:PUBLIC", "from:2", "to:4", "group:spring-boot", "action:upgrade"),
            )
            val matcher = RecipeMatcher(
                FakeRecipeSource(
                    FakeRecipeSource.Companion.SPRING_BOOT_3_TEST_RECIPE,
                    FakeRecipeSource.Companion.SPRING_BOOT_4_TEST_RECIPE,
                    directRecipe,
                ),
            )

            // when
            val recipeDescriptors = matcher.findMatchingByTargetVersion(
                "spring-boot",
                "upgrade",
                Version.of(4L),
            )

            // then
            recipeDescriptors shouldBeEqual listOf(directRecipe.descriptor)
        }

        test("should return empty list when target version has no matching recipes") {
            // when
            val recipeDescriptors = recipeMatcher.findMatchingByTargetVersion(
                "spring-boot",
                "upgrade",
                Version.of(999L),
            )

            // then
            recipeDescriptors shouldBeEqual emptyList()
        }
    }
}

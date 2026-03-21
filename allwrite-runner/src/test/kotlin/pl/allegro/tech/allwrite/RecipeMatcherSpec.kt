package pl.allegro.tech.allwrite

import com.github.zafarkhaja.semver.Version
import io.kotest.matchers.equals.shouldBeEqual
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.base.BaseRunnerSpec
import pl.allegro.tech.allwrite.common.fake.FakeRecipe
import pl.allegro.tech.allwrite.common.fake.FakeRecipeSource
import pl.allegro.tech.allwrite.common.fake.FakeRuntimeModule
import pl.allegro.tech.allwrite.common.fake.FakeRecipeSource.Companion.SPRING_BOOT_3_TEST_RECIPE
import pl.allegro.tech.allwrite.common.fake.FakeRecipeSource.Companion.SPRING_BOOT_4_TEST_RECIPE
import pl.allegro.tech.allwrite.common.port.incoming.RecipeCoordinates
import pl.allegro.tech.allwrite.common.util.injectEagerly
import pl.allegro.tech.allwrite.runner.application.RecipeMatcher

class RecipeMatcherSpec : BaseRunnerSpec() {

    override fun additionalModules() = listOf(
        FakeRuntimeModule().module
    )

    private val recipeMatcher: RecipeMatcher by injectEagerly()

    init {
        test("should return exact match when present") {
            val recipeDescriptors = recipeMatcher.findMatching(
                RecipeCoordinates("spring-boot", "upgrade", Version.of(3L), Version.of(4L))
            )

            recipeDescriptors shouldBeEqual listOf(SPRING_BOOT_4_TEST_RECIPE.descriptor)
        }

        test("should return all matching when no exact match and version not specified") {
            val recipeDescriptors = recipeMatcher.findMatching(
                RecipeCoordinates("spring-boot", "upgrade", null, null)
            )

            recipeDescriptors shouldBeEqual listOf(SPRING_BOOT_3_TEST_RECIPE.descriptor, SPRING_BOOT_4_TEST_RECIPE.descriptor)
        }

        test("should return empty list when no exact match and versions are specified") {
            val recipeDescriptors = recipeMatcher.findMatching(
                RecipeCoordinates("spring-boot", "upgrade", Version.of(777L), Version.of(999L))
            )

            recipeDescriptors shouldBeEqual emptyList()
        }

        test("should return chain of recipes when only target version is provided") {
            val recipeDescriptors = recipeMatcher.findMatchingByTargetVersion(
                "spring-boot", "upgrade", Version.of(4L)
            )

            recipeDescriptors shouldBeEqual listOf(
                SPRING_BOOT_3_TEST_RECIPE.descriptor,
                SPRING_BOOT_4_TEST_RECIPE.descriptor
            )
        }

        test("should return direct recipe when it covers the full range") {
            val directRecipe = FakeRecipe(
                id = "pl.allegro.tech.allwrite.recipes.spring-boot-2-to-4",
                tags = setOf("visibility:PUBLIC", "from:2", "to:4", "group:spring-boot", "action:upgrade")
            )
            val matcher = RecipeMatcher(
                FakeRecipeSource(
                    SPRING_BOOT_3_TEST_RECIPE,
                    SPRING_BOOT_4_TEST_RECIPE,
                    directRecipe
                )
            )

            val recipeDescriptors = matcher.findMatchingByTargetVersion(
                "spring-boot", "upgrade", Version.of(4L)
            )

            recipeDescriptors shouldBeEqual listOf(directRecipe.descriptor)
        }

        test("should return empty list when target version has no matching recipes") {
            val recipeDescriptors = recipeMatcher.findMatchingByTargetVersion(
                "spring-boot", "upgrade", Version.of(999L)
            )

            recipeDescriptors shouldBeEqual emptyList()
        }
    }
}

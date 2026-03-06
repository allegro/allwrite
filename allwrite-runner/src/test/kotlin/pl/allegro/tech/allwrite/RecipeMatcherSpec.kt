package pl.allegro.tech.allwrite

import com.github.zafarkhaja.semver.Version
import io.kotest.matchers.equals.shouldBeEqual
import org.koin.core.component.inject
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.base.BaseRunnerSpec
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
    }
}

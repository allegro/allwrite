package pl.allegro.tech.allwrite.runtime

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import org.koin.test.inject
import pl.allegro.tech.allwrite.api.RecipeSource
import pl.allegro.tech.allwrite.runtime.base.BaseRuntimeSpec
import pl.allegro.tech.allwrite.runtime.fake.FakeExternalRecipeProvider
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class RecipeSourceSpec : BaseRuntimeSpec() {
    init {
        val recipeSource: RecipeSource by inject()

        test("should find all allegro recipes with public visibility") {
            // when
            val recipes = recipeSource.findAll()

            // then
            val recipeIds = recipes.map { it.name }
            recipeIds shouldContainAll listOf(
                "pl.allegro.tech.allwrite.recipes.KotlinPublicRecipe",
                "pl.allegro.tech.allwrite.recipes.JavaPublicRecipe",
                "pl.allegro.tech.allwrite.recipes.YamlPublicRecipe",
            )
            recipeIds shouldNotContainAnyOf listOf(
                "pl.allegro.tech.allwrite.recipes.KotlinInternalRecipe",
                "pl.allegro.tech.allwrite.recipes.JavaInternalRecipe",
                "pl.allegro.tech.allwrite.recipes.YamlInternalRecipe",
                "pl.allegro.tech.allwrite.recipes.Java3rdPartyRecipe",
            )
        }

        test("should resolve OpenRewrite recipes included in external declarative recipe") {
            // given
            val externalJar = createExternalRecipeJar()
            val recipeSource = OpenrewriteRecipeSource(FakeExternalRecipeProvider(listOf(externalJar)))

            // when
            val recipe = recipeSource.get(EXTERNAL_COMPOSITE_RECIPE)

            // then
            recipe.recipeList.map { it.name } shouldContain "org.openrewrite.text.Find"
        }
    }

    private fun createExternalRecipeJar(): Path {
        val jarPath = Files.createTempFile("external-recipe", ".jar")
            .also { it.toFile().deleteOnExit() }
        JarOutputStream(Files.newOutputStream(jarPath)).use { jar ->
            jar.putNextEntry(JarEntry("META-INF/rewrite/external-recipe.yml"))
            jar.write(
                """
                type: specs.openrewrite.org/v1beta/recipe
                name: $EXTERNAL_COMPOSITE_RECIPE
                displayName: External composite recipe
                recipeList:
                  - org.openrewrite.text.Find:
                      find: needle
                """.trimIndent().toByteArray(UTF_8),
            )
            jar.closeEntry()
        }
        return jarPath
    }

    private companion object {
        const val EXTERNAL_COMPOSITE_RECIPE = "pl.allegro.tech.autoupgrades.recipes.ExternalCompositeRecipe"
    }
}

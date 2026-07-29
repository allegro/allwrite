package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.INTERNAL
import pl.allegro.tech.allwrite.recipes.java.ChangeType

internal class RelocateAutoConfigureWebTestClient :
    AllwriteRecipe(
        displayName = "Relocate Spring Boot test client types",
        description = "Moves Spring Boot test client types to their Spring Boot 4 packages.",
        visibility = INTERNAL,
    ),
    ClasspathAwareRecipe {
    override fun requireOnClasspath(): List<String> = listOf("spring-boot-test-3")

    override fun getRecipeList(): List<Recipe> =
        RELOCATIONS.map { (from, to) ->
            ChangeType(from, to, false)
        }

    private companion object {
        val RELOCATIONS = listOf(
            "org.springframework.boot.test.web.reactive.server.WebTestClientBuilderCustomizer" to
                "org.springframework.boot.webtestclient.autoconfigure.WebTestClientBuilderCustomizer",
            "org.springframework.boot.test.web.client.TestRestTemplate" to
                "org.springframework.boot.resttestclient.TestRestTemplate",
        )
    }
}

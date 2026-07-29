package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.INTERNAL
import pl.allegro.tech.allwrite.recipes.util.FindAndReplace

internal class RelocateAutoConfigureWebTestClient :
    AllwriteRecipe(
        displayName = "Relocate AutoConfigureWebTestClient",
        description = "Moves AutoConfigureWebTestClient to its Spring Boot 4 package.",
        visibility = INTERNAL,
    ) {
    override fun getRecipeList(): List<Recipe> =
        listOf(
            FindAndReplace(
                find = "org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient",
                replace = "org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient",
                regex = false,
                caseSensitive = true,
                multiline = false,
                dotAll = false,
                filePattern = null,
                plaintextOnly = false,
            ),
        )
}

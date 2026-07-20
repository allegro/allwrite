package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.Recipe
import org.openrewrite.text.FindAndReplace
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.INTERNAL

internal class ChangeSpringBoot4MongoProperties :
    AllwriteRecipe(
        displayName = "Change Spring Boot MongoDB property keys",
        description = "Changes Spring Boot MongoDB property keys removed in Spring Boot 4.",
        visibility = INTERNAL,
    ) {

    override fun getRecipeList(): List<Recipe> =
        listOf(
            FindAndReplace("spring.data.mongodb.uri", "spring.mongodb.uri", false, true, false, false, null, false),
            FindAndReplace("spring.data.mongodb.database", "spring.mongodb.database", false, true, false, false, null, false),
        )
}

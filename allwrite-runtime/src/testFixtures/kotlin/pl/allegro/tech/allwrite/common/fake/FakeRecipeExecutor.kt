package pl.allegro.tech.allwrite.common.fake

import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.common.port.incoming.RecipeExecutor
import java.nio.file.Path

@Single
class FakeRecipeExecutor : RecipeExecutor {

    val executedRecipes = mutableListOf<Recipe>()

    override fun execute(recipe: Recipe, inputFiles: List<Path>, failOnError: Boolean) {
        println("Executing ${recipe.name}")
        executedRecipes.add(recipe)
    }
}

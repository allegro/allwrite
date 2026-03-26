package pl.allegro.tech.allwrite.runtime.fake

import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.spi.AllwriteRecipe
import pl.allegro.tech.allwrite.spi.RecipeVisibility.INTERNAL

open class FakeCompositeRecipe(
    val childRecipes: List<Recipe>
) : AllwriteRecipe(visibility = INTERNAL) {

    constructor(vararg childRecipes: Recipe) : this(childRecipes.toList())

    override fun getRecipeList(): List<Recipe> = childRecipes
}

package pl.allegro.tech.allwrite.common.fake

import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.INTERNAL

open class FakeCompositeRecipe(
    val childRecipes: List<Recipe>
) : AllwriteRecipe(visibility = INTERNAL) {

    constructor(vararg childRecipes: Recipe) : this(childRecipes.toList())

    override fun getRecipeList(): List<Recipe> = childRecipes
}

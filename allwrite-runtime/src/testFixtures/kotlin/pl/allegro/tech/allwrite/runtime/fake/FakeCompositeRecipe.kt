package pl.allegro.tech.allwrite.runtime.fake

import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.AllwriteRecipe

open class FakeCompositeRecipe(
    val childRecipes: List<Recipe>,
) : AllwriteRecipe() {

    constructor(vararg childRecipes: Recipe) : this(childRecipes.toList())

    override fun getRecipeList(): List<Recipe> = childRecipes
}

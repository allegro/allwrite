package pl.allegro.tech.allwrite.recipes

import org.openrewrite.Recipe

class KotlinPublicRecipe : Recipe() {

    override fun getDisplayName() = "Public Kotlin recipe"

    override fun getDescription() = "Public Kotlin recipe description."

    override fun getTags() = setOf("visibility:${RecipeVisibility.PUBLIC}")
}

package pl.allegro.tech.allwrite.recipes

import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.spi.RecipeVisibility

class KotlinPublicRecipe : Recipe() {

    override fun getDisplayName() = "Public Kotlin recipe"

    override fun getDescription() = "Public Kotlin recipe description."

    override fun getTags() = setOf("visibility:${RecipeVisibility.PUBLIC}")
}

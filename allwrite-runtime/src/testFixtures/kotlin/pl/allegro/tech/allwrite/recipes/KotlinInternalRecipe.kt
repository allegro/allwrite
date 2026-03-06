package pl.allegro.tech.allwrite.recipes

import org.openrewrite.Recipe

class KotlinInternalRecipe : Recipe() {

    override fun getDisplayName() = "Internal Kotlin recipe"

    override fun getDescription() = "Internal Kotlin recipe description."

    override fun getTags() = setOf("visibility:internal")
}

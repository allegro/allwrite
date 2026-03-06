package pl.allegro.tech.allwrite.recipes

import org.openrewrite.Recipe

public abstract class AllwriteRecipe(
    displayName: String? = null,
    description: String? = null,
    public val visibility: RecipeVisibility,
    public val group: String? = null,
    public val recipe: String? = null
) : Recipe() {

    private val metadata = RecipeMetadata(displayName, description, visibility, group, recipe)

    override fun getDisplayName(): String = metadata.displayName
    override fun getDescription(): String = metadata.description
    override fun getTags(): Set<String> = metadata.tags
}

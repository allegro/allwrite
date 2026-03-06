package pl.allegro.tech.allwrite.recipes

import org.openrewrite.ScanningRecipe

public abstract class AllwriteScanningRecipe<T : Any>(
    displayName: String? = null,
    description: String? = null,
    public val visibility: RecipeVisibility = RecipeVisibility.INTERNAL,
    public val group: String? = null,
    public val recipe: String? = null,
) : ScanningRecipe<T>() {

    private val metadata = RecipeMetadata(displayName, description, visibility, group, recipe)

    override fun getDisplayName(): String = metadata.displayName
    override fun getDescription(): String = metadata.description
    override fun getTags(): Set<String> = metadata.tags
}

package pl.allegro.tech.allwrite

import org.openrewrite.ScanningRecipe

public abstract class AllwriteScanningRecipe<T : Any>(
    displayName: String? = null,
    description: String? = null,
    visibility: RecipeVisibility = RecipeVisibility.INTERNAL,
    group: String? = null,
    recipe: String? = null,
    from: String? = null,
    to: String? = null,
) : ScanningRecipe<T>() {

    private val metadata = RecipeMetadata(displayName, description, visibility, group, recipe, from, to)

    override fun getDisplayName(): String = metadata.displayName
    override fun getDescription(): String = metadata.description
    override fun getTags(): Set<String> = metadata.tags
}

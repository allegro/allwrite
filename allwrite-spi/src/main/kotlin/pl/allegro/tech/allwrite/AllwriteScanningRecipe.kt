package pl.allegro.tech.allwrite

import org.openrewrite.ScanningRecipe

public abstract class AllwriteScanningRecipe<T : Any> @JvmOverloads public constructor(
    displayName: String? = null,
    description: String? = null,
    from: String? = null,
    to: String? = null,
    dependabotArtifacts: List<String> = emptyList(),
) : ScanningRecipe<T>() {

    private val metadata =
        RecipeMetadata(displayName, description, from, to, dependabotArtifacts)

    override fun getDisplayName(): String = metadata.displayName
    override fun getDescription(): String = metadata.description
    public override fun getTags(): Set<String> = metadata.tags
}

package pl.allegro.tech.allwrite

import org.openrewrite.Recipe

public abstract class AllwriteRecipe @JvmOverloads public constructor(
    displayName: String? = null,
    description: String? = null,
    public val visibility: RecipeVisibility,
    public val group: String? = null,
    public val action: String? = null,
    public val from: String? = null,
    public val to: String? = null,
    public val dependabotArtifacts: List<String> = emptyList(),
) : Recipe() {

    public val metadata: RecipeMetadata =
        RecipeMetadata(displayName, description, visibility, group, action, from, to, dependabotArtifacts)

    override fun getDisplayName(): String = metadata.displayName
    override fun getDescription(): String = metadata.description
    override fun getTags(): Set<String> = metadata.tags
}

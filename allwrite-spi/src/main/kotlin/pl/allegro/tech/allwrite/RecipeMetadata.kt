package pl.allegro.tech.allwrite

public class RecipeMetadata @JvmOverloads public constructor(
    displayName: String?,
    description: String?,
    public val from: String?,
    public val to: String?,
    public val dependabotArtifacts: List<String> = emptyList(),
) {
    public val displayName: String = displayName ?: javaClass.simpleName
    public val description: String = description ?: "${this.displayName}."

    public val tags: Set<String> = buildSet {
        from?.let { add("from:$it") }
        to?.let { add("to:$it") }
        dependabotArtifacts.forEach { add("dependabot-artifact:$it") }
    }
}

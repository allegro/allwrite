package pl.allegro.tech.allwrite

public abstract class CliAllwriteScanningRecipe<T : Any> @JvmOverloads public constructor(
    public val group: String,
    public val action: String,
    displayName: String? = null,
    description: String? = null,
    from: String? = null,
    to: String? = null,
    dependabotArtifacts: List<String> = emptyList(),
) : AllwriteScanningRecipe<T>(displayName, description, from, to, dependabotArtifacts) {

    init {
        require(group.isNotBlank()) { "CLI recipes must specify a group." }
        require(action.isNotBlank()) { "CLI recipes must specify an action." }
    }

    final override fun getTags(): Set<String> = super.getTags() + setOf("group:$group", "action:$action")
}

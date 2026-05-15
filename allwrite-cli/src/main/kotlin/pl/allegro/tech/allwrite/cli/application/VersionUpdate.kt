package pl.allegro.tech.allwrite.cli.application

import kotlinx.serialization.Serializable
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.api.RecipeCoordinates
import pl.allegro.tech.allwrite.api.tagPropertyOrNull
import com.github.zafarkhaja.semver.Version as DomainVersion

@Serializable
internal data class PullRequestManagerExtras(
    val dependabot: List<VersionUpdate>,
)

@Serializable
internal data class VersionUpdate(
    val artifact: String,
    val from: Version,
    val to: Version,
) {
    fun toRecipeCoordinates(recipes: List<RecipeDescriptor>): RecipeCoordinates? {
        val matched = recipes.firstOrNull { it.tags.contains("dependabot-artifact:$artifact") } ?: return null
        return RecipeCoordinates(
            group = matched.tagPropertyOrNull("group") ?: return null,
            action = matched.tagPropertyOrNull("action") ?: return null,
            fromVersion = DomainVersion.parse(from.normalVersion),
            toVersion = DomainVersion.parse(to.normalVersion),
        )
    }
}

@Serializable
internal data class Version(
    val normalVersion: String,
)

package pl.allegro.tech.allwrite.runner.application

import com.github.zafarkhaja.semver.Version as DomainVersion
import kotlinx.serialization.Serializable
import pl.allegro.tech.allwrite.common.port.incoming.RecipeCoordinates

private val GROUPS_BY_ARTIFACT = mapOf<String, String>()

@Serializable
internal data class PullRequestManagerExtras(val dependabot: List<VersionUpdate>)

@Serializable
internal data class VersionUpdate(val artifact: String, val from: Version, val to: Version) {

    fun toRecipeCoordinates() =
        GROUPS_BY_ARTIFACT[artifact]?.let { RecipeCoordinates(it, "upgrade", DomainVersion.parse(from.normalVersion), DomainVersion.parse(to.normalVersion)) }
}

@Serializable
internal data class Version(val normalVersion: String)

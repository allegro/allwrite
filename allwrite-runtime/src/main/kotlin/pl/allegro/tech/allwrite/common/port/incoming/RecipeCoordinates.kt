package pl.allegro.tech.allwrite.common.port.incoming

import com.github.zafarkhaja.semver.Version
import org.openrewrite.config.RecipeDescriptor

public data class RecipeCoordinates(
    val group: String,
    val recipe: String,
    val fromVersion: Version?,
    val toVersion: Version?
) {

    public val canonicalName: String
        get() = "$group/$recipe"

    override fun toString(): String =
        buildString {
            append(group)
            append("/")
            append(recipe)
            if (fromVersion != null) {
                append(" $fromVersion")
                if (toVersion != null) {
                    append(" $toVersion")
                }
            }
        }
}

public fun RecipeDescriptor.toRecipeCoordinatesOrNull(): RecipeCoordinates? =
    runCatching {
        RecipeCoordinates(
            group = tagProperty("group"),
            recipe = tagProperty("recipe"),
            fromVersion = getFromVersion(),
            toVersion = getToVersion()
        )
    }.getOrNull()

public fun RecipeDescriptor.tagPropertyOrNull(key: String): String? =
    tags.find { it.startsWith(key) }?.substringAfter(":")

public fun RecipeDescriptor.tagProperty(key: String): String =
    tagPropertyOrNull(key) ?: error("Tag property '$key' is missing for recipe $name")

public fun RecipeDescriptor.getFromVersion(): Version? =
    tagPropertyOrNull("from")?.let { Version.parse(it, false) }

public fun RecipeDescriptor.getToVersion(): Version? =
    tagPropertyOrNull("to")?.let { Version.parse(it, false) }

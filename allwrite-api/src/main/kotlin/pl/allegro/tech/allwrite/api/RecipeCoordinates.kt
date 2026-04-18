package pl.allegro.tech.allwrite.api

import com.github.zafarkhaja.semver.Version
import org.openrewrite.config.RecipeDescriptor

public data class RecipeCoordinates(
    val group: String,
    val action: String,
    val fromVersion: Version?,
    val toVersion: Version?,
) {

    public val canonicalName: String
        get() = "$group/$action"

    override fun toString(): String =
        buildString {
            append(group)
            append("/")
            append(action)
            if (fromVersion != null) {
                append(" ${fromVersion.toCompactString()}")
                if (toVersion != null) {
                    append(" ${toVersion.toCompactString()}")
                }
            }
        }
}

public fun RecipeDescriptor.toRecipeCoordinatesOrNull(): RecipeCoordinates? =
    runCatching {
        RecipeCoordinates(
            group = tagProperty("group"),
            action = tagProperty("action"),
            fromVersion = getFromVersion(),
            toVersion = getToVersion(),
        )
    }.getOrNull()

public fun RecipeDescriptor.tagPropertyOrNull(key: String): String? = tags.find { it.startsWith(key) }?.substringAfter(":")

public fun RecipeDescriptor.tagProperty(key: String): String = tagPropertyOrNull(key) ?: error("Tag property '$key' is missing for recipe $name")

public fun RecipeDescriptor.getFromVersion(): Version? = tagPropertyOrNull("from")?.let { Version.parse(it, false) }

public fun RecipeDescriptor.getToVersion(): Version? = tagPropertyOrNull("to")?.let { Version.parse(it, false) }

public fun Version.toCompactString(): String =
    buildString {
        append(majorVersion())
        if (minorVersion() != 0L || patchVersion() != 0L) {
            append(".${minorVersion()}")
            if (patchVersion() != 0L) {
                append(".${patchVersion()}")
            }
        }
    }

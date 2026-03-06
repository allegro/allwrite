package pl.allegro.tech.allwrite.recipes.util

import org.openrewrite.text.Find as RewriteFind

/**
 * Kotlin-friendly wrapper for [org.openrewrite.text.Find]
 */
internal fun Find(
    pattern: String,
    regex: Boolean? = null,
    caseSensitive: Boolean? = null,
    multiline: Boolean? = null,
    dotAll: Boolean? = null,
    filePattern: String? = null,
    description: Boolean? = null,
    contextSize: Int? = null
) =
    RewriteFind(pattern, regex, caseSensitive, multiline, dotAll, filePattern, description, contextSize)

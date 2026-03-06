package pl.allegro.tech.allwrite.recipes.util

/**
 * Kotlin-friendly wrapper for [org.openrewrite.text.FindAndReplace]
 */
internal fun FindAndReplace(
    find: String,
    replace: String,
    regex: Boolean,
    caseSensitive: Boolean,
    multiline: Boolean,
    dotAll: Boolean,
    filePattern: String?,
    plaintextOnly: Boolean,
) = org.openrewrite.text.FindAndReplace(
    find,
    replace,
    regex,
    caseSensitive,
    multiline,
    dotAll,
    filePattern,
    plaintextOnly
)

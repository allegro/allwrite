package pl.allegro.tech.allwrite.recipes.util

internal fun String.globToTokenRegex(): String {
    val thisString = this
    return buildString {
        thisString.forEach { char ->
            when (char) {
                '*' -> append("[^'\",:\\s]*")
                '?' -> append("[^'\",:\\s]")
                else -> append(Regex.escape(char.toString()))
            }
        }
    }
}

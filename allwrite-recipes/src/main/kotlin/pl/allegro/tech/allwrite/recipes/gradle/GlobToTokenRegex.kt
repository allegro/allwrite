package pl.allegro.tech.allwrite.recipes.gradle

internal fun globToTokenRegex(pattern: String): String =
    buildString {
        pattern.forEach { char ->
            when (char) {
                '*' -> append("[^'\",:\\s]*")
                '?' -> append("[^'\",:\\s]")
                else -> append(Regex.escape(char.toString()))
            }
        }
    }

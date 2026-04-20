package pl.allegro.tech.allwrite.recipes.java

import org.openrewrite.java.tree.J

internal fun <T : J> List<T>.spacesInBetween(): List<T> =
    mapIndexed { index, e ->
        if (index != 0) {
            e.withPrefix(e.prefix.withWhitespace(e.prefix.whitespace + " "))
        } else {
            e
        }
    }

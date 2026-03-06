package pl.allegro.tech.allwrite.recipes.yaml

import org.openrewrite.yaml.tree.Yaml

/**
 * Represents OpenRewrite's `prefix` as a:
 *
 * - [remainder] - part of the prefix before first `\n`, in fact a postfix for the previous line
 * - [indent] - part of the prefix after the last `\n`, in fact an indent for the current line
 * - [main] - anything in between, must not contain `\n` in the beginning or the end
 */
internal data class PrefixParts(val remainder: String?, val main: String?, val indent: String) {

    fun merge(another: PrefixParts): PrefixParts {
        val mergedBody = when {
            this.main == null && another.main == null -> null
            this.main == null -> another.main
            another.main == null -> this.main
            else -> "${this.main}\n${another.main}"
        }
        return PrefixParts(this.remainder, mergedBody, another.indent)
    }

    /**
     * Returns prefix as a string, ensuring that;
     *
     * - [remainder] is still a postfix for the previous line
     * - [main] is separated from [remainder] with [lineBreaksBefore] line breaks
     * - [indent] is separated from [main] with [lineBreaksAfter] line breaks
     */
    fun asString(lineBreaksBefore: Int = 1, lineBreaksAfter: Int = 1, keepNewLinesInMain: Boolean = false) =
        buildString {
            val hasRemainder = remainder != null
            val hasMain = main != null && (keepNewLinesInMain || main.trim().isNotEmpty())
            if (hasRemainder) {
                append(remainder)
            }
            if (hasRemainder || hasMain) {
                append("\n".repeat(lineBreaksBefore))
            }
            if (hasMain) {
                val m = main!!.lines()
                    .filter { keepNewLinesInMain || it.trim().isNotEmpty() }
                    .joinToString("\n")
                append(m)
                append("\n".repeat(lineBreaksAfter))
            }
            append(indent)
        }

    companion object {

        val EMPTY = PrefixParts("", "", "")
    }
}

internal fun Yaml.prefixParts(): PrefixParts = this.prefix.asPrefixParts()

// Document.End does not contain a trailing \n, so its parsing should be different
internal fun Yaml.Document.End.prefixParts(): PrefixParts {
    val lines = this.prefix.lines()
    return when (lines.size) {
        in Int.MIN_VALUE..0 -> PrefixParts.EMPTY
        1 -> PrefixParts(remainder = lines[0], main = null, indent = "")
        else -> PrefixParts(
            remainder = lines[0],
            main = lines.drop(1).joinToString("\n"),
            indent = ""
        )
    }
}

internal fun String.asPrefixParts(): PrefixParts {
    val lines = this.lines()
    return when (lines.size) {
        in Int.MIN_VALUE..0 -> PrefixParts.EMPTY
        1 -> PrefixParts(remainder = null, main = null, indent = lines[0])
        2 -> PrefixParts(remainder = lines[0], main = null, indent = lines[1])
        else -> PrefixParts(
            remainder = lines[0],
            main = lines.slice(1 until lines.size - 1).joinToString("\n"),
            indent = lines.last()
        )
    }
}

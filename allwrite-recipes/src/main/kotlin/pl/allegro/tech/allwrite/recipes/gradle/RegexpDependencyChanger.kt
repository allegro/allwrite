package pl.allegro.tech.allwrite.recipes.gradle

import java.util.regex.Matcher
import java.util.regex.Pattern

internal class RegexpDependencyChanger(
    oldGroupId: String,
    oldArtifactId: String,
    private val newGroupId: String,
    private val newArtifactId: String,
    private val newVersion: String?,
) {
    private data class ReplacementStrategy(
        val name: String,
        val pattern: Pattern,
        val includeVersionKey: Boolean,
        val includeVersion: Boolean,
    )

    private val replacementStrategies: List<ReplacementStrategy> = listOf(
        ReplacementStrategy(
            name = "version-key-value",
            pattern = Pattern.compile(
                "(?<group>${Pattern.quote(oldGroupId)})" +
                    "(?<separator1>['\",:\\s]+)" +
                    "(?<nameKey>name[:=\\s'\"]+)?" +
                    "(?<artifactId>${Pattern.quote(oldArtifactId)})" +
                    "(?<separator2>['\",:\\s]+)" +
                    "(?<versionKey>version(\\.ref)?[:=\\s'\"]+)" +
                    "(?<version>[^('|\")]+)",
                Pattern.MULTILINE,
            ),
            includeVersionKey = true,
            includeVersion = true,
        ),
        ReplacementStrategy(
            name = "version-value",
            pattern = Pattern.compile(
                "(?<group>${Pattern.quote(oldGroupId)})" +
                    "(?<separator1>['\",:\\s]+)" +
                    "(?<nameKey>name[:=\\s'\"]+)?" +
                    "(?<artifactId>${Pattern.quote(oldArtifactId)})" +
                    "(?<separator2>['\",:\\s]+)" +
                    "(?<version>[^()'\"\\s,}]+)",
                Pattern.MULTILINE,
            ),
            includeVersionKey = false,
            includeVersion = true,
        ),
        ReplacementStrategy(
            name = "versionless",
            pattern = Pattern.compile(
                "(?<group>${Pattern.quote(oldGroupId)})" +
                    "(?<separator1>['\",:\\s]+)" +
                    "(?<nameKey>name[:=\\s'\"]+)?" +
                    "(?<artifactId>${Pattern.quote(oldArtifactId)})" +
                    "(?<separator2>['\",:\\s]+)" +
                    "(?=[,)\\]}\\s]|$)",
                Pattern.MULTILINE,
            ),
            includeVersionKey = false,
            includeVersion = false,
        ),
    )

    fun update(originalText: String): String =
        replacementStrategies.fold(originalText) { currentText, strategy ->
            replaceMatchingDeclaration(currentText, strategy)
        }

    private fun replaceMatchingDeclaration(originalText: String, strategy: ReplacementStrategy): String {
        val matcher = strategy.pattern.matcher(originalText)
        if (!matcher.find()) {
            return originalText
        }

        val updatedText = StringBuffer()
        do {
            val shouldWriteVersion = strategy.includeVersion && newVersion != null
            val replacement =
                buildString {
                    append(newGroupId)
                    append(matcher.group("separator1"))
                    append(matcher.group("nameKey") ?: "")
                    append(newArtifactId)
                    if (shouldWriteVersion || !strategy.includeVersion) {
                        append(matcher.group("separator2"))
                    } else {
                        append(trimVersionSeparator(matcher.group("separator2")))
                    }
                    if (strategy.includeVersionKey && shouldWriteVersion) {
                        append(matcher.group("versionKey") ?: "")
                    }
                    if (shouldWriteVersion) {
                        append(newVersion)
                    }
                }
            matcher.appendReplacement(updatedText, Matcher.quoteReplacement(replacement))
        } while (matcher.find())

        matcher.appendTail(updatedText)
        return updatedText.toString()
    }

    private fun trimVersionSeparator(separator: String): String = separator.replace(Regex("[,:\\s]+$"), "")
}

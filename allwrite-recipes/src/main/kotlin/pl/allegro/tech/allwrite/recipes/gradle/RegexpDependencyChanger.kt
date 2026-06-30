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
    private enum class RuleType {
        VERSION_KEY,
        VERSION_VALUE,
        VERSIONLESS,
    }

    private data class Rule(
        val type: RuleType,
        val pattern: Pattern,
    )

    private val rules: List<Rule> = listOf(
        Rule(
            type = RuleType.VERSION_KEY,
            pattern = Pattern.compile(
                "(?<group>${Pattern.quote(oldGroupId)})" +
                    "(?<separator1>['\",:\\s]+)" +
                    "(?<nameKey>name[:=\\s'\"]+)?" +
                    "(?<artifactId>${Pattern.quote(oldArtifactId)})" +
                    "(?<separator2>['\",:\\s]+)" +
                    "(?<versionKey>version(\\.ref)?[:=\\s]+)" +
                    "(?<versionQuote>['\"]?)" +
                    "(?<version>\\$\\{[^}]+}|[^()'\"\\s,}]+)" +
                    "['\"]?",
                Pattern.MULTILINE,
            ),
        ),
        Rule(
            type = RuleType.VERSION_VALUE,
            pattern = Pattern.compile(
                "(?<group>${Pattern.quote(oldGroupId)})" +
                    "(?<separator1>['\",:\\s]+)" +
                    "(?<nameKey>name[:=\\s'\"]+)?" +
                    "(?<artifactId>${Pattern.quote(oldArtifactId)})" +
                    "(?<separator2>['\",:\\s]+)" +
                    "(?<version>\\$\\{[^}]+}|[^()'\"\\s,}]+)",
                Pattern.MULTILINE,
            ),
        ),
        Rule(
            type = RuleType.VERSIONLESS,
            pattern = Pattern.compile(
                "(?<group>${Pattern.quote(oldGroupId)})" +
                    "(?<separator1>['\",:\\s]+)" +
                    "(?<nameKey>name[:=\\s'\"]+)?" +
                    "(?<artifactId>${Pattern.quote(oldArtifactId)})" +
                    "(?<separator2>['\",:\\s]+)" +
                    "(?=[,)\\]}\\s]|$)",
                Pattern.MULTILINE,
            ),
        ),
    )

    fun update(originalText: String): String =
        rules.fold(originalText) { currentText, rule ->
            applyRule(currentText, rule)
        }

    private fun applyRule(originalText: String, rule: Rule): String {
        val matcher = rule.pattern.matcher(originalText)
        if (!matcher.find()) {
            return originalText
        }

        val updatedText = StringBuffer()
        do {
            val replacement = buildReplacement(matcher, rule.type)
            matcher.appendReplacement(updatedText, Matcher.quoteReplacement(replacement))
        } while (matcher.find())

        matcher.appendTail(updatedText)
        return updatedText.toString()
    }

    private fun buildReplacement(matcher: Matcher, type: RuleType): String {
        val separator2 = matcher.group("separator2")
        val separatorBeforeVersion = if (newVersion == null && type != RuleType.VERSIONLESS) trimVersionSeparator(separator2) else separator2

        return buildString {
            append(newGroupId)
            append(matcher.group("separator1"))
            append(matcher.group("nameKey") ?: "")
            append(newArtifactId)
            append(separatorBeforeVersion)

            if (newVersion != null && type == RuleType.VERSION_KEY) {
                append(matcher.group("versionKey") ?: "")
            }

            if (newVersion != null && type != RuleType.VERSIONLESS) {
                if (type == RuleType.VERSION_KEY) {
                    append(matcher.group("versionQuote").orDoubleQuote())
                }
                append(newVersion)
                if (type == RuleType.VERSION_KEY) {
                    append(matcher.group("versionQuote").orDoubleQuote())
                }
            }
        }
    }

    private fun String?.orDoubleQuote(): String = if (isNullOrEmpty()) "\"" else this

    private fun trimVersionSeparator(separator: String): String = separator.replace(Regex("[,:\\s]+$"), "")
}

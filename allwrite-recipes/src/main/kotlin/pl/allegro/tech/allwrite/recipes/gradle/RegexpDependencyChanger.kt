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
    private val regexpDependencyDeclaration =
        "(?<group>${Pattern.quote(oldGroupId)})" +
            "(?<separator1>['\",:\\s]+)" +
            "(?<nameKey>name[:=\\s'\"]+)?" +
            "(?<artifactId>${Pattern.quote(oldArtifactId)})" +
            "(?<separator2>['\",:\\s]+)" +
            "(?<versionKey>version(.ref)?[:=\\s'\"]+)?" +
            "(?<version>[^('|\")]+)"

    fun update(originalText: String): String {
        val matcher = Pattern.compile(regexpDependencyDeclaration, Pattern.MULTILINE).matcher(originalText)
        if (!matcher.find()) {
            return originalText
        }

        val updatedText = StringBuffer()
        do {
            val replacement =
                buildString {
                    append(newGroupId)
                    append(matcher.group("separator1"))
                    append(matcher.group("nameKey") ?: "")
                    append(newArtifactId)
                    append(matcher.group("separator2"))
                    append(matcher.group("versionKey") ?: "")
                    append(newVersion ?: matcher.group("version"))
                }
            matcher.appendReplacement(updatedText, Matcher.quoteReplacement(replacement))
        } while (matcher.find())

        matcher.appendTail(updatedText)
        return updatedText.toString()
    }
}

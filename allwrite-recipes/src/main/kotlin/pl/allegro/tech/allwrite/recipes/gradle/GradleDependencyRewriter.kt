package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.SourceFile
import org.openrewrite.text.PlainTextParser

internal class GradleDependencyRewriter(
    private val oldGroupId: String,
    private val oldArtifactId: String,
    private val newGroupId: String,
    private val newArtifactId: String,
    private val newVersion: String?,
    private val regexpDependencyChanger: RegexpDependencyChanger,
) {
    fun update(sourceFile: SourceFile): SourceFile {
        val plainText = PlainTextParser.convert(sourceFile)
        val rewrittenText = updateText(plainText.text)
        return if (rewrittenText == plainText.text) sourceFile else plainText.withText(rewrittenText)
    }

    private fun updateText(originalText: String): String {
        val interpolation = rewriteGroovyVersionInterpolation(originalText)
        val versionedText = regexpDependencyChanger.update(interpolation.text)
        return interpolation.existingVersionVariable?.let { existingVersionVariable ->
            val version = newVersion ?: return@let versionedText
            addGroovyVersionVariable(versionedText, existingVersionVariable, newArtifactId.toVersionVariableName(), version)
        } ?: versionedText
    }

    private fun rewriteGroovyVersionInterpolation(originalText: String): GroovyInterpolationRewrite {
        val versionVariable = newArtifactId.toVersionVariableName()
        val interpolationPattern =
            Regex(
                """(?m)^(?<indent>\s*)(?<configuration>[A-Za-z_][A-Za-z0-9_]*)\s+group:\s*['"]${Regex.escape(oldGroupId)}['"],""" +
                    """\s*name:\s*['"]${Regex.escape(oldArtifactId)}['"],""" +
                    """\s*version:\s*"\$\{(?<versionVariable>[A-Za-z_][A-Za-z0-9_]*)}"\s*$""",
            )

        var matchedVersionVariable: String? = null
        val updatedText = interpolationPattern.replace(originalText) { matchResult ->
            matchedVersionVariable = matchedVersionVariable ?: matchResult.groups["versionVariable"]?.value
            val indent = matchResult.groups["indent"]?.value.orEmpty()
            val configuration = matchResult.groups["configuration"]?.value.orEmpty()
            buildString {
                append(indent)
                append(configuration)
                append(" group: '")
                append(newGroupId)
                append("', name: '")
                append(newArtifactId)
                if (newVersion != null) {
                    append("', version: \"")
                    append('$')
                    append('{')
                    append(versionVariable)
                    append("}\"")
                } else {
                    append("'")
                }
            }
        }

        val existingVersionVariable =
            matchedVersionVariable?.takeIf { newVersion != null }
                ?: return GroovyInterpolationRewrite(updatedText, null)
        return GroovyInterpolationRewrite(updatedText, existingVersionVariable)
    }

    private fun addGroovyVersionVariable(originalText: String, existingVersionVariable: String, newVersionVariable: String, version: String): String {
        if (groovyVersionVariableExists(originalText, newVersionVariable)) return originalText
        val pattern =
            Regex(
                """(?m)^(?<indent>\s*)${Regex.escape(existingVersionVariable)}\s*=\s*['"][^'"]*['"]\s*$""",
            )
        val match = pattern.find(originalText) ?: return originalText
        val indent = match.groups["indent"]?.value.orEmpty()
        val inserted = "$indent$newVersionVariable = '$version'"
        return buildString {
            append(originalText, 0, match.range.last + 1)
            append('\n')
            append(inserted)
            append(originalText, match.range.last + 1, originalText.length)
        }
    }

    private fun groovyVersionVariableExists(originalText: String, versionVariable: String): Boolean =
        Regex("""(?m)^\s*${Regex.escape(versionVariable)}\s*=\s*['"][^'"]*['"]\s*$""").containsMatchIn(originalText)

    private fun String.toVersionVariableName(): String {
        val sanitized = replace(Regex("[^A-Za-z0-9_]"), "_")
        return if (sanitized.firstOrNull()?.isDigit() == true) "_$sanitized" else sanitized
    }
}

private data class GroovyInterpolationRewrite(
    val text: String,
    val existingVersionVariable: String?,
)

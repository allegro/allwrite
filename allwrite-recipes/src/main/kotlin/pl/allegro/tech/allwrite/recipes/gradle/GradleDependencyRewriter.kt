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
        val restoredText = interpolation.replacementLine?.let { versionedText.replace(INTERPOLATED_DEPENDENCY_PLACEHOLDER, it) } ?: versionedText

        return interpolation.existingVersionVariable?.let { existingVersionVariable ->
            val version = newVersion ?: return@let restoredText
            addGroovyVersionVariable(restoredText, existingVersionVariable, newArtifactId.toVersionVariableName(), version)
        } ?: restoredText
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
        var matchedIndent: String? = null
        var matchedConfiguration: String? = null
        val updatedText = interpolationPattern.replace(originalText) { matchResult ->
            matchedVersionVariable = matchedVersionVariable ?: matchResult.groups["versionVariable"]?.value
            matchedIndent = matchedIndent ?: matchResult.groups["indent"]?.value
            matchedConfiguration = matchedConfiguration ?: matchResult.groups["configuration"]?.value
            INTERPOLATED_DEPENDENCY_PLACEHOLDER
        }

        val existingVersionVariable = matchedVersionVariable ?: return GroovyInterpolationRewrite(originalText, null, null)
        val indent = matchedIndent ?: return GroovyInterpolationRewrite(originalText, null, null)
        val configuration = matchedConfiguration ?: return GroovyInterpolationRewrite(originalText, null, null)
        val replacementLine = buildString {
            append(indent)
            append(configuration)
            append(" group: '")
            append(newGroupId)
            append("', name: '")
            append(newArtifactId)
            append("', version: \"")
            append('$')
            append('{')
            append(versionVariable)
            append("}\"")
        }
        return GroovyInterpolationRewrite(updatedText, existingVersionVariable, replacementLine)
    }

    private fun addGroovyVersionVariable(originalText: String, existingVersionVariable: String, newVersionVariable: String, version: String): String {
        if (originalText.contains("$newVersionVariable =")) return originalText
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

    private fun String.toVersionVariableName(): String = replace('-', '_')
}

private data class GroovyInterpolationRewrite(
    val text: String,
    val existingVersionVariable: String?,
    val replacementLine: String?,
)

private const val INTERPOLATED_DEPENDENCY_PLACEHOLDER = "__ALLWRITE_INTERPOLATED_DEPENDENCY__"

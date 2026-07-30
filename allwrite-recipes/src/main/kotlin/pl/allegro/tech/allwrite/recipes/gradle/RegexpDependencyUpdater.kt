package pl.allegro.tech.allwrite.recipes.gradle

import com.github.zafarkhaja.semver.Version
import pl.allegro.tech.allwrite.recipes.util.globToTokenRegex
import java.util.regex.Matcher
import java.util.regex.Pattern

internal class RegexpDependencyUpdater(
    groupId: String,
    artifactId: String,
    targetVersion: String,
    private val versionPattern: String,
) {
    private val normalizedTargetVersion = targetVersion.trim()
    private val targetSemver = parseSemver(normalizedTargetVersion) ?: error("Invalid target version: $targetVersion")

    /** Regular expression matching dependency declaration in following formats:
     * - classpath("GROUP:ID:1.4.46")
     * - classpath("GROUP", "ID", "1.4.40")
     * - classpath group: 'GROUP', name: 'ID', version: '1.4.32'
     * - classpath(group = "GROUP", name = "ID", version = "1.4.42")
     * - classpath group: 'pl.allegro.tech.phoenix', name: 'phoenix-provisioning-plugin', version: "$variableName"
     * - phoenix-provisioning-plugin = { group = "GROUP", name = "ID", version.ref = "propertyName" } // TOML format
     */
    private val regexpDependencyDeclaration =
        "(?<group>${Regex.escape(groupId)})" +
            "(?<separator1>['\",:\\s]+)" +
            "(?<nameKey>name[:=\\s'\"]+)?" +
            "(?<artifactId>${artifactId.globToTokenRegex()})" +
            "(?<separator2>['\",:\\s]+)" +
            "(?<versionKey>version(.ref)?[:=\\s'\"]+)?" +
            "(?<version>[^('|\")]+)"

    fun update(originalText: String): String {
        val patternOptions = Pattern.MULTILINE
        val compiledVersionPattern = Pattern.compile(versionPattern)
        val matcher = Pattern
            .compile(regexpDependencyDeclaration, patternOptions)
            .matcher(originalText)

        if (!matcher.find()) {
            return originalText
        }

        val (textWithDirectVersionsReplaced, variableNamesRequiringUpdate) = replaceDirectVersions(matcher, compiledVersionPattern)

        return variableNamesRequiringUpdate.fold(textWithDirectVersionsReplaced) { text, versionFound ->
            updateVersionInVariable(text, versionFound, patternOptions, compiledVersionPattern)
        }
    }

    private data class ReplacementResult(
        val textWithDirectVersionsReplaced: String,
        val variableNamesRequiringUpdate: List<String>,
    )

    private fun replaceDirectVersions(matcher: Matcher, compiledVersionPattern: Pattern): ReplacementResult {
        val result = StringBuffer()
        val variableVersionsToUpdate = mutableListOf<String>()

        do {
            val versionFound = matcher.group("version")
            val versionMatcher = compiledVersionPattern.matcher(versionFound)
            val currentSemver = parseSemver(versionFound)

            if (versionMatcher.find() && currentSemver != null && !targetSemver.isLowerThan(currentSemver)) {
                val replacement = matcher.group("group") +
                    matcher.group("separator1") +
                    (matcher.group("nameKey") ?: "") +
                    matcher.group("artifactId") +
                    matcher.group("separator2") +
                    (matcher.group("versionKey") ?: "") +
                    normalizedTargetVersion
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement))
            } else {
                variableVersionsToUpdate.add(versionFound)
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()))
            }
        } while (matcher.find())

        matcher.appendTail(result)
        return ReplacementResult(result.toString(), variableVersionsToUpdate)
    }

    private fun updateVersionInVariable(originalText: String, versionFound: String, patternOptions: Int, versionPattern: Pattern): String {
        val maybeLocalVariable =
            versionFound
                .trimStart('$')
                .trimStart('{')
                .trimEnd('}')

        if (maybeLocalVariable.endsWith("()")) {
            // it's not a local variable, but a function call, functions are not supported
            return originalText
        }

        // try to find the declaration and value of the local variable
        val searchVariableDeclaration =
            "(?<variableName>$maybeLocalVariable)(?<separator>[=:'\"\\s]+)(?<version>[^(\"|')]+)"
        val variableMatcher = Pattern.compile(searchVariableDeclaration, patternOptions).matcher(originalText)

        if (!variableMatcher.find()) {
            // didn't find the variable declaration
            return originalText
        }

        if (!versionPattern.matcher(variableMatcher.group("version")).find()) {
            // variable value doesn't match version pattern
            return originalText
        }

        val currentSemver = parseSemver(variableMatcher.group("version")) ?: return originalText

        if (targetSemver.isLowerThan(currentSemver)) {
            return originalText
        }

        return variableMatcher.replaceFirst($$"${variableName}${separator}$$normalizedTargetVersion")
    }

    private fun parseSemver(version: String): Version? =
        runCatching {
            Version.parse(version.trim(), false)
        }.getOrNull()
}

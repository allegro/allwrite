package pl.allegro.tech.allwrite.recipes.gradle.dependencyupdate

import java.util.regex.Pattern

internal class RegexpDependencyUpdater(
    groupId: String,
    artifactId: String,
    private val targetVersion: String,
    private val versionPattern: String,
    private val multiline: Boolean,
) {
    /** Regular expression matching dependency declaration in following formats:
     * - classpath("GROUP:ID:1.4.46")
     * - classpath("GROUP", "ID", "1.4.40")
     * - classpath group: 'GROUP', name: 'ID', version: '1.4.32'
     * - classpath(group = "GROUP", name = "ID", version = "1.4.42")
     * - classpath group: 'pl.allegro.tech.phoenix', name: 'phoenix-provisioning-plugin', version: "$variableName"
     * - phoenix-provisioning-plugin = { group = "GROUP", name = "ID", version.ref = "propertyName" } // TOML format
     */
    private val regexpDependencyDeclaration =
        "(?<group>$groupId)" +
            "(?<separator1>['\",:\\s]+)" +
            "(?<nameKey>name[:=\\s'\"]+)?" +
            "(?<artifactId>$artifactId)" +
            "(?<separator2>['\",:\\s]+)" +
            "(?<versionKey>version(.ref)?[:=\\s'\"]+)?" +
            "(?<version>[^('|\")]+)"

    fun update(originalText: String): String {
        var patternOptions = 0
        if (multiline) {
            patternOptions = patternOptions or Pattern.MULTILINE
        }
        val matcher =
            Pattern
                .compile(regexpDependencyDeclaration, patternOptions)
                .matcher(originalText)

        if (!matcher.find()) {
            // didn't find a dependency declaration matching specified regular expression
            return originalText
        }

        val versionFound = matcher.group("version")
        val versionPattern = Pattern.compile(versionPattern)
        val versionMatcher = versionPattern.matcher(versionFound)

        return if (versionMatcher.find()) {
            // version found and it matches version pattern, replace the version
            matcher.replaceFirst("\${group}\${separator1}\${nameKey}\${artifactId}\${separator2}\${versionKey}$targetVersion")
        } else {
            updateVersionInVariable(originalText, versionFound, patternOptions, versionPattern)
        }
    }

    private fun updateVersionInVariable(
        originalText: String,
        versionFound: String,
        patternOptions: Int,
        versionPattern: Pattern,
    ): String {
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

        return variableMatcher.replaceFirst("\${variableName}\${separator}$targetVersion")
    }
}

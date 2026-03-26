package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.FindSourceFiles
import org.openrewrite.Option
import org.openrewrite.Preconditions
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.text.PlainTextParser
import pl.allegro.tech.allwrite.spi.AllwriteRecipe
import pl.allegro.tech.allwrite.spi.RecipeVisibility.INTERNAL

public class UpdateGradleDependency(
    @Option
    private val groupId: String = "",
    @Option
    private val artifactId: String = "",
    @Option
    private val targetVersion: String = "",
    @Option
    private val sourceVersionPattern: String = "\\d+.\\d+.\\d+",
    @Option
    private val filePatterns: List<String> = listOf(
        "{**/,}*.gradle",
        "{**/,}*.gradle.kts",
        "{**/,}gradle/*.toml",
    ),
) : AllwriteRecipe(
    displayName = "Dependency updater using regular expression",
    description = """Updates specified dependency using regular expressions.
        |"Supports dependency declarations in the following formats:" +
        | - classpath("GROUP:ID:1.4.46")
        | - classpath("GROUP", "ID", "1.4.40")
        | - classpath group: 'GROUP', name: 'ID', version: '1.4.32'
        | - classpath(group = "GROUP", name = "ID", version = "1.4.42")
        |
        |Versions declared in simple variables are supported as well:
        | - classpath group: 'GROUP', name: 'ID', version: "${'$'}versionInVariable" +
        | - plugin-name = { group = "GROUP", name = "ID", version.ref = \"versionInVariable\" } // TOML format.
        """.trimMargin(),
    visibility = INTERNAL
) {

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        Preconditions.check(
            filePatternPreconditions(),
            DependencyUpdaterVisitor(groupId, artifactId, targetVersion, sourceVersionPattern)
        )

    private fun filePatternPreconditions(): TreeVisitor<*, ExecutionContext> =
        Preconditions.or(*filePatterns
            .map { FindSourceFiles(it) }
            .map { it.visitor }
            .toTypedArray())

    internal class DependencyUpdaterVisitor(
        groupId: String,
        artifactId: String,
        targetVersion: String,
        versionPattern: String
    ) : TreeVisitor<Tree, ExecutionContext>() {

        private val regexpDependencyUpdater = RegexpDependencyUpdater(
            groupId = groupId,
            artifactId = artifactId,
            targetVersion = targetVersion,
            versionPattern = versionPattern
        );

        override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
            val plainText = PlainTextParser.convert(tree as SourceFile)
            val updatedText = regexpDependencyUpdater.update(plainText.text)

            return if (updatedText != plainText.text) {
                plainText.withText(updatedText)
            } else {
                tree
            }
        }
    }
}

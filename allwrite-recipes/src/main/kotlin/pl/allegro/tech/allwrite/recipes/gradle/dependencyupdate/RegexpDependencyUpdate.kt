package pl.allegro.tech.allwrite.recipes.gradle.dependencyupdate

import org.openrewrite.ExecutionContext
import org.openrewrite.FindSourceFiles
import org.openrewrite.Option
import org.openrewrite.Preconditions
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.text.PlainTextParser
import pl.allegro.tech.allwrite.recipes.AllwriteRecipe
import pl.allegro.tech.allwrite.recipes.RecipeVisibility.INTERNAL

public class RegexpDependencyUpdate(
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
    @Option
    private val multiline: Boolean = true,
    @Option
    private val updateOnlyWhenMainRecipeMadeChanges: Boolean = false
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

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        val allPreconditions = if (updateOnlyWhenMainRecipeMadeChanges) {
            Preconditions.and(
                ConditionalChangeVisitor(),
                filePatternPreconditions()
            )
        } else {
            filePatternPreconditions()
        }

        return Preconditions.check(
            allPreconditions,
            DependencyUpdaterVisitor(groupId, artifactId, targetVersion, sourceVersionPattern, multiline)
        )
    }

    private fun filePatternPreconditions(): TreeVisitor<*, ExecutionContext> =
        Preconditions.or(*filePatterns
            .map { FindSourceFiles(it) }
            .map { it.visitor }
            .toTypedArray())

    internal class DependencyUpdaterVisitor(
        groupId: String,
        artifactId: String,
        targetVersion: String,
        versionPattern: String,
        multiline: Boolean
    ) : TreeVisitor<Tree, ExecutionContext>() {

        private val regexpDependencyUpdater = RegexpDependencyUpdater(
            groupId, artifactId, targetVersion,
            versionPattern, multiline
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

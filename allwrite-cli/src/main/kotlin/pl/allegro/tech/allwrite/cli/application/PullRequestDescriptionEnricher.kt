package pl.allegro.tech.allwrite.cli.application

import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.cli.application.port.outgoing.PullRequestContext

@Single
internal class PullRequestDescriptionEnricher(
    private val pullRequestContext: PullRequestContext
) {

    fun addRewriteDisclaimerToPullRequest(executedRecipes: List<Recipe>) {
        if (executedRecipes.isEmpty()) return

        val body = pullRequestContext.getDescription()
        val (existingDisclaimer, originalDescription) = body?.splitByLastSeparator(DISCLAIMER_SEPARATOR) ?: Pair("", "")

        val newDisclaimer = buildDisclaimer(existingDisclaimer, executedRecipes)
        val updatedDescription = UPDATED_PR_DESCRIPTION_TEMPLATE.format(newDisclaimer, originalDescription)
        pullRequestContext.updateDescription(updatedDescription)
    }

    private fun buildDisclaimer(existingDisclaimer: String, executedRecipes: List<Recipe>): String {
        val newRecipeInfo = buildRecipeInfoText(executedRecipes)
        val previousMigrationsDescriptions = MIGRATION_DESCRIPTION_PATTERN.findAll(existingDisclaimer).map { it.value }
        val previousMigrationsCount = previousMigrationsDescriptions.count()

        val newMigrationDescription =
            """
            |### Migration ${previousMigrationsCount + 1}
            |
            |$newRecipeInfo
            |
            |[comment]: # (END_OF_MIGRATION_DESCRIPTION ${previousMigrationsCount + 1})
            """.trimMargin()

        return (previousMigrationsDescriptions + newMigrationDescription).joinToString("\n\n")
    }

    private fun buildRecipeInfoText(executedRecipes: List<Recipe>): String = executedRecipes
        .distinctBy { it.name }
        .withIndex()
        .joinToString("\n\n") { (index, recipe) ->
            """
            |#### Recipe ${index + 1}: `${recipe.displayName}`
            |
            |${recipe.description}
            """.trimMargin()
        }

    private fun String.splitByLastSeparator(separator: String): Pair<String, String> =
        substringBeforeLast(separator, missingDelimiterValue = "").trim() to substringAfterLast(separator).trim()

    companion object {

        private const val DISCLAIMER_SEPARATOR = """<hr id="auto-upgrade-watchdog-separator"/>"""

        private val MIGRATION_DESCRIPTION_PATTERN =
            """### Migration \d+\n\n.*?\n\n\[comment\]: # \(END_OF_MIGRATION_DESCRIPTION .+?\)""".toRegex(RegexOption.DOT_MATCHES_ALL)

        val UPDATED_PR_DESCRIPTION_TEMPLATE =
            """
            |## 🤖 Allwrite bot has taken over this PR! 🤖
            |
            |%s
            |
            |$DISCLAIMER_SEPARATOR
            |
            |%s
            """.trimMargin()
    }
}

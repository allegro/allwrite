package pl.allegro.tech.allwrite.cli

import io.kotest.matchers.shouldBe
import org.koin.core.module.Module
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.cli.application.PullRequestDescriptionEnricher
import pl.allegro.tech.allwrite.cli.application.port.outgoing.PullRequestContext
import pl.allegro.tech.allwrite.cli.base.BaseCliSpec
import pl.allegro.tech.allwrite.cli.fake.github.FakeGithubModule
import pl.allegro.tech.allwrite.cli.fake.github.FakePullRequestContext
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipe
import pl.allegro.tech.allwrite.runtime.util.injectEagerly

class PullRequestDescriptionEnricherSpec : BaseCliSpec() {

    private val pullRequestDescriptionEnricher: PullRequestDescriptionEnricher by injectEagerly()
    private val fakePullRequestContext: PullRequestContext by injectEagerly()

    override fun additionalModules(): List<Module> = listOf(
        FakeGithubModule().module,
    )

    init {
        test("should be noop when no recipes executed") {
            // when
            pullRequestDescriptionEnricher.addRewriteDisclaimerToPullRequest(emptyList())

            // then
            fakePullRequestContext.getDescription() shouldBe FakePullRequestContext.Companion.ORIGINAL_DESCRIPTION
        }

        test("should prepend description with recipe info") {
            // when
            pullRequestDescriptionEnricher.addRewriteDisclaimerToPullRequest(listOf(FakeRecipe()))

            // then
            fakePullRequestContext.getDescription() shouldBe """
                ## 🤖 Allwrite bot has taken over this PR! 🤖

                ### Migration 1

                #### Recipe 1: `Fake recipe`

                Fake recipe description.

                [comment]: # (END_OF_MIGRATION_DESCRIPTION 1)

                <hr id="auto-upgrade-watchdog-separator"/>

                ${FakePullRequestContext.Companion.ORIGINAL_DESCRIPTION}
                """.trimIndent()
        }

        test("should deduplicate recipes") {
            // when
            pullRequestDescriptionEnricher.addRewriteDisclaimerToPullRequest(listOf(FakeRecipe(), FakeRecipe()))

            // then
            fakePullRequestContext.getDescription() shouldBe """
                ## 🤖 Allwrite bot has taken over this PR! 🤖

                ### Migration 1

                #### Recipe 1: `Fake recipe`

                Fake recipe description.

                [comment]: # (END_OF_MIGRATION_DESCRIPTION 1)

                <hr id="auto-upgrade-watchdog-separator"/>

                ${FakePullRequestContext.Companion.ORIGINAL_DESCRIPTION}
                """.trimIndent()
        }

        test("should append recipe info to the disclaimer in the description when it is already present") {
            // given
            fakePullRequestContext.updateDescription("""
                ## 🤖 Allwrite bot has taken over this PR! 🤖

                ### Migration 1

                #### Recipe 1: `Fake recipe`

                Fake recipe description.

                [comment]: # (END_OF_MIGRATION_DESCRIPTION 1)

                <hr id="auto-upgrade-watchdog-separator"/>

                ${FakePullRequestContext.Companion.ORIGINAL_DESCRIPTION}
                """.trimIndent()
            )

            // when
            pullRequestDescriptionEnricher.addRewriteDisclaimerToPullRequest(
                listOf(
                    FakeRecipe(),
                    FakeRecipe(id = "recipe2", displayName = "recipe2", description = "second recipe")
                )
            )

            // then
            fakePullRequestContext.getDescription() shouldBe """
                ## 🤖 Allwrite bot has taken over this PR! 🤖

                ### Migration 1

                #### Recipe 1: `Fake recipe`

                Fake recipe description.

                [comment]: # (END_OF_MIGRATION_DESCRIPTION 1)

                ### Migration 2

                #### Recipe 1: `Fake recipe`

                Fake recipe description.

                #### Recipe 2: `recipe2`

                second recipe

                [comment]: # (END_OF_MIGRATION_DESCRIPTION 2)

                <hr id="auto-upgrade-watchdog-separator"/>

                ${FakePullRequestContext.Companion.ORIGINAL_DESCRIPTION}
                """.trimIndent()
        }
    }
}

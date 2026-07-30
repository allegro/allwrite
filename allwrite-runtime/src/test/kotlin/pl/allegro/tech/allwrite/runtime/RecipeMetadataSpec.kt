package pl.allegro.tech.allwrite.runtime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import pl.allegro.tech.allwrite.RecipeMetadata
import pl.allegro.tech.allwrite.RecipeVisibility

class RecipeMetadataSpec : FunSpec() {
    init {
        test("should produce dependabot-artifact tag when dependabotArtifacts is provided") {
            // given
            val metadata = RecipeMetadata(
                displayName = "Test",
                description = "Test.",
                visibility = RecipeVisibility.PUBLIC,
                group = "test-group",
                action = "upgrade",
                from = "1",
                to = "2",
                dependabotArtifacts = listOf("org.example:lib"),
            )

            // expect
            metadata.tags shouldContain "dependabot-artifact:org.example:lib"
        }

        test("should produce no dependabot-artifact tags when dependabotArtifacts is empty") {
            // given
            val metadata = RecipeMetadata(
                displayName = "Test",
                description = "Test.",
                visibility = RecipeVisibility.PUBLIC,
                group = "test-group",
                action = "upgrade",
                from = "1",
                to = "2",
                dependabotArtifacts = emptyList(),
            )

            // expect
            metadata.tags.none { it.startsWith("dependabot-artifact:") } shouldBe true
        }

        test("should produce multiple dependabot-artifact tags for multiple artifacts") {
            // given
            val metadata = RecipeMetadata(
                displayName = "Test",
                description = "Test.",
                visibility = RecipeVisibility.PUBLIC,
                group = "test-group",
                action = "upgrade",
                from = "1",
                to = "2",
                dependabotArtifacts = listOf("org.example:lib-a", "org.example:lib-b"),
            )

            // expect
            metadata.tags shouldContainAll listOf(
                "dependabot-artifact:org.example:lib-a",
                "dependabot-artifact:org.example:lib-b",
            )
        }

        test("should require a group for public CLI recipes") {
            // given
            val metadata = {
                RecipeMetadata(
                    displayName = "Test",
                    description = "Test.",
                    visibility = RecipeVisibility.PUBLIC,
                    group = null,
                    action = "upgrade",
                    from = "1",
                    to = "2",
                )
            }

            // when
            val exception = shouldThrow<IllegalStateException> { metadata() }

            // then
            exception.message shouldBe "Public CLI recipes must specify a group."
        }

        test("should require an action for public CLI recipes") {
            // given
            val metadata = {
                RecipeMetadata(
                    displayName = "Test",
                    description = "Test.",
                    visibility = RecipeVisibility.PUBLIC,
                    group = "test-group",
                    action = null,
                    from = "1",
                    to = "2",
                )
            }

            // when
            val exception = shouldThrow<IllegalStateException> { metadata() }

            // then
            exception.message shouldBe "Public CLI recipes must specify an action."
        }
    }
}

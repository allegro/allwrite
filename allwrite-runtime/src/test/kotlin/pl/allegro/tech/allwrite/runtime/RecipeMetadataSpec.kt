package pl.allegro.tech.allwrite.runtime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import pl.allegro.tech.allwrite.RecipeMetadata
import pl.allegro.tech.allwrite.RecipeVisibility

class RecipeMetadataSpec : FunSpec() {
    init {
        test("should produce dependabot-artifact tag when dependabotArtifacts is provided") {
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

            metadata.tags shouldContain "dependabot-artifact:org.example:lib"
        }

        test("should produce no dependabot-artifact tags when dependabotArtifacts is empty") {
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

            metadata.tags.none { it.startsWith("dependabot-artifact:") } shouldBe true
        }

        test("should produce multiple dependabot-artifact tags for multiple artifacts") {
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

            metadata.tags shouldContainAll listOf(
                "dependabot-artifact:org.example:lib-a",
                "dependabot-artifact:org.example:lib-b",
            )
        }
    }
}

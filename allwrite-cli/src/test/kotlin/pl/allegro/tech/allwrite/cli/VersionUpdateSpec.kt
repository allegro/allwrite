package pl.allegro.tech.allwrite.cli

import com.github.zafarkhaja.semver.Version
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.api.RecipeCoordinates
import pl.allegro.tech.allwrite.cli.application.VersionUpdate
import java.net.URI
import pl.allegro.tech.allwrite.cli.application.Version as VersionDto

class VersionUpdateSpec : FunSpec() {
    init {
        test("should resolve coordinates when matching dependabot-artifact tag exists") {
            val update = VersionUpdate("org.example:lib", VersionDto("2.7.0"), VersionDto("3.0.0"))
            val recipes = listOf(
                RecipeDescriptor(setOf("group:my-group", "action:upgrade", "dependabot-artifact:org.example:lib")),
            )

            update.toRecipeCoordinates(recipes) shouldBe RecipeCoordinates(
                group = "my-group",
                action = "upgrade",
                fromVersion = Version.parse("2.7.0"),
                toVersion = Version.parse("3.0.0"),
            )
        }

        test("should return null when no recipe has matching dependabot-artifact tag") {
            val update = VersionUpdate("org.example:lib", VersionDto("1.0.0"), VersionDto("2.0.0"))
            val recipes = listOf(
                RecipeDescriptor(setOf("group:other", "action:upgrade", "dependabot-artifact:org.example:other-lib")),
            )

            update.toRecipeCoordinates(recipes) shouldBe null
        }

        test("should return null when recipe list is empty") {
            val update = VersionUpdate("org.example:lib", VersionDto("1.0.0"), VersionDto("2.0.0"))

            update.toRecipeCoordinates(emptyList()) shouldBe null
        }

        test("should match first recipe when multiple recipes have matching tag") {
            val update = VersionUpdate("org.example:lib", VersionDto("1.0.0"), VersionDto("2.0.0"))
            val recipes = listOf(
                RecipeDescriptor(setOf("group:first-group", "action:upgrade", "dependabot-artifact:org.example:lib")),
                RecipeDescriptor(setOf("group:second-group", "action:upgrade", "dependabot-artifact:org.example:lib")),
            )

            update.toRecipeCoordinates(recipes)?.group shouldBe "first-group"
        }
    }
}

@Suppress("FunctionNaming")
private fun RecipeDescriptor(tags: Set<String>) =
    RecipeDescriptor(
        "name",
        "display name",
        "instance name",
        "description",
        tags,
        null,
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        URI("file:///not-used"),
    )

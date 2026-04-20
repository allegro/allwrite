package pl.allegro.tech.allwrite.cli

import com.github.ajalt.clikt.testing.test
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.cli.application.AddExternalRecipeCommand
import pl.allegro.tech.allwrite.cli.application.ListExternalRecipesCommand
import pl.allegro.tech.allwrite.cli.application.RemoveExternalRecipeCommand
import pl.allegro.tech.allwrite.cli.application.UpdateExternalRecipeCommand
import pl.allegro.tech.allwrite.cli.base.BaseCliSpec
import pl.allegro.tech.allwrite.cli.fake.os.FakeJarFetcher
import pl.allegro.tech.allwrite.cli.fake.os.FakeOperatingSystemModule
import pl.allegro.tech.allwrite.runtime.fake.FakeRuntimeModule
import pl.allegro.tech.allwrite.runtime.port.outgoing.ExternalRecipeProvider
import pl.allegro.tech.allwrite.runtime.util.injectEagerly

class ExternalRecipeCommandsSpec : BaseCliSpec() {

    private val addCommand: AddExternalRecipeCommand by injectEagerly()
    private val updateCommand: UpdateExternalRecipeCommand by injectEagerly()
    private val removeCommand: RemoveExternalRecipeCommand by injectEagerly()
    private val listCommand: ListExternalRecipesCommand by injectEagerly()
    private val fakeJarFetcher: FakeJarFetcher by injectEagerly()
    private val externalRecipeProvider: ExternalRecipeProvider by injectEagerly()

    override fun additionalModules() =
        listOf(
            FakeRuntimeModule().module,
            FakeOperatingSystemModule().module,
        )

    init {
        test("add should fetch jar and register external recipe") {
            val result = addCommand.test("my-recipes https://repo.com/recipes-1.0.jar")

            result.statusCode shouldBe 0
            fakeJarFetcher.fetchedJars shouldHaveSize 1
            fakeJarFetcher.fetchedJars[0].url shouldBe "https://repo.com/recipes-1.0.jar"
        }

        test("add should fail when name already exists") {
            addCommand.test("my-recipes https://repo.com/recipes-1.0.jar")

            val result = addCommand.test("my-recipes https://repo.com/recipes-2.0.jar")

            result.statusCode shouldBe 1
            result.output shouldContain "already exists"
        }

        test("update should re-fetch jar with new url") {
            addCommand.test("my-recipes https://repo.com/recipes-1.0.jar")

            val result = updateCommand.test("my-recipes https://repo.com/recipes-2.0.jar")

            result.statusCode shouldBe 0
            fakeJarFetcher.fetchedJars shouldHaveSize 2
            fakeJarFetcher.fetchedJars[1].url shouldBe "https://repo.com/recipes-2.0.jar"
        }

        test("update should fail when name does not exist") {
            val result = updateCommand.test("nonexistent https://repo.com/recipes.jar")

            result.statusCode shouldBe 1
            result.output shouldContain "not found"
        }

        test("rm should remove external recipe and delete jar") {
            addCommand.test("my-recipes https://repo.com/recipes-1.0.jar")

            val result = removeCommand.test("my-recipes")

            result.statusCode shouldBe 0
            externalRecipeProvider.get() shouldHaveSize 0
        }

        test("rm should fail when name does not exist") {
            val result = removeCommand.test("nonexistent")

            result.statusCode shouldBe 1
            result.output shouldContain "not found"
        }

        test("update without url should re-fetch jar from stored url") {
            addCommand.test("my-recipes https://repo.com/recipes-1.0.jar")

            val result = updateCommand.test("my-recipes")

            result.statusCode shouldBe 0
            fakeJarFetcher.fetchedJars shouldHaveSize 2
            fakeJarFetcher.fetchedJars[1].url shouldBe "https://repo.com/recipes-1.0.jar"
        }

        test("update without url should fail when name does not exist") {
            val result = updateCommand.test("nonexistent")

            result.statusCode shouldBe 1
            result.output shouldContain "not found"
        }

        test("add should make jar discoverable to runtime module") {
            addCommand.test("my-recipes https://repo.com/recipes-1.0.jar")

            val jarPaths = externalRecipeProvider.get()

            jarPaths shouldHaveSize 1
            jarPaths[0].fileName.toString() shouldBe "my-recipes.jar"
        }

        test("ls should list all external recipes with urls") {
            addCommand.test("my-recipes https://repo.com/recipes-1.0.jar")
            addCommand.test("other-recipes https://repo.com/other-2.0.jar")

            val result = listCommand.test("")

            result.statusCode shouldBe 0
            result.output shouldContain "my-recipes"
            result.output shouldContain "https://repo.com/recipes-1.0.jar"
            result.output shouldContain "other-recipes"
            result.output shouldContain "https://repo.com/other-2.0.jar"
        }

        test("ls should show message when no external recipes configured") {
            val result = listCommand.test("")

            result.statusCode shouldBe 0
            result.output shouldContain "No external recipes configured."
        }
    }
}

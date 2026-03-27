package pl.allegro.tech.allwrite.cli

import com.github.ajalt.clikt.testing.test
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.cli.application.AddExternalRecipeCommand
import pl.allegro.tech.allwrite.cli.application.RefreshExternalRecipeCommand
import pl.allegro.tech.allwrite.cli.application.RemoveExternalRecipeCommand
import pl.allegro.tech.allwrite.cli.application.UpdateExternalRecipeCommand
import pl.allegro.tech.allwrite.cli.base.BaseCliSpec
import pl.allegro.tech.allwrite.cli.fake.os.FakeJarDownloader
import pl.allegro.tech.allwrite.cli.fake.os.FakeOperatingSystemModule
import pl.allegro.tech.allwrite.cli.infrastructure.os.ExternalRecipeStore
import pl.allegro.tech.allwrite.runtime.fake.FakeRuntimeModule
import pl.allegro.tech.allwrite.runtime.util.injectEagerly

class ExternalRecipeCommandsSpec : BaseCliSpec() {

    private val addCommand: AddExternalRecipeCommand by injectEagerly()
    private val updateCommand: UpdateExternalRecipeCommand by injectEagerly()
    private val removeCommand: RemoveExternalRecipeCommand by injectEagerly()
    private val refreshCommand: RefreshExternalRecipeCommand by injectEagerly()
    private val fakeJarDownloader: FakeJarDownloader by injectEagerly()
    private val externalRecipeStore: ExternalRecipeStore by injectEagerly()

    override fun additionalModules() = listOf(
        FakeRuntimeModule().module,
        FakeOperatingSystemModule().module,
    )

    init {
        test("add should download jar and register external recipe") {
            val result = addCommand.test("my-recipes https://repo.com/recipes-1.0.jar")

            result.statusCode shouldBe 0
            result.output shouldContain "Added external recipe 'my-recipes'"
            fakeJarDownloader.downloadedJars shouldHaveSize 1
            fakeJarDownloader.downloadedJars[0].url shouldBe "https://repo.com/recipes-1.0.jar"
        }

        test("add should fail when name already exists") {
            addCommand.test("my-recipes https://repo.com/recipes-1.0.jar")

            val result = addCommand.test("my-recipes https://repo.com/recipes-2.0.jar")

            result.statusCode shouldBe 1
            result.output shouldContain "already exists"
        }

        test("update should re-download jar with new url") {
            addCommand.test("my-recipes https://repo.com/recipes-1.0.jar")

            val result = updateCommand.test("my-recipes https://repo.com/recipes-2.0.jar")

            result.statusCode shouldBe 0
            result.output shouldContain "Updated external recipe 'my-recipes'"
            fakeJarDownloader.downloadedJars shouldHaveSize 2
            fakeJarDownloader.downloadedJars[1].url shouldBe "https://repo.com/recipes-2.0.jar"
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
            result.output shouldContain "Removed external recipe 'my-recipes'"
            externalRecipeStore.getJarPaths() shouldHaveSize 0
        }

        test("rm should fail when name does not exist") {
            val result = removeCommand.test("nonexistent")

            result.statusCode shouldBe 1
            result.output shouldContain "not found"
        }

        test("refresh should re-download jar from stored url") {
            addCommand.test("my-recipes https://repo.com/recipes-1.0.jar")

            val result = refreshCommand.test("my-recipes")

            result.statusCode shouldBe 0
            result.output shouldContain "Refreshed external recipe 'my-recipes'"
            fakeJarDownloader.downloadedJars shouldHaveSize 2
            fakeJarDownloader.downloadedJars[1].url shouldBe "https://repo.com/recipes-1.0.jar"
        }

        test("refresh should fail when name does not exist") {
            val result = refreshCommand.test("nonexistent")

            result.statusCode shouldBe 1
            result.output shouldContain "not found"
        }

        test("add should make jar discoverable via getJarPaths") {
            addCommand.test("my-recipes https://repo.com/recipes-1.0.jar")

            val jarPaths = externalRecipeStore.getJarPaths()

            jarPaths shouldHaveSize 1
            jarPaths[0].fileName.toString() shouldBe "my-recipes.jar"
        }
    }
}

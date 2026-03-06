package pl.allegro.tech.allwrite

import com.github.ajalt.clikt.testing.test
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.koin.ksp.generated.module
import org.koin.test.get
import pl.allegro.tech.allwrite.base.BaseRunnerSpec
import pl.allegro.tech.allwrite.common.fake.FakeRuntimeModule
import pl.allegro.tech.allwrite.common.util.injectEagerly
import pl.allegro.tech.allwrite.fake.external.FakeTelemetryPublisher
import pl.allegro.tech.allwrite.fake.os.FakeGitMetadata
import pl.allegro.tech.allwrite.fake.os.FakeOperatingSystemModule
import pl.allegro.tech.allwrite.fake.os.FakeSystemInfo
import pl.allegro.tech.allwrite.runner.application.ListRecipesCommand
import pl.allegro.tech.allwrite.runner.application.RunCommand
import pl.allegro.tech.allwrite.runner.application.RunRecipeCommand
import pl.allegro.tech.allwrite.runner.application.port.outgoing.Telemetry.CommandOutcome.SUCCESS
import java.time.Instant
import kotlin.time.Duration

class TelemetryRecordingSpec : BaseRunnerSpec() {

    private val listRecipesCommand: ListRecipesCommand by injectEagerly()
    private val runRecipeCommand: RunRecipeCommand by injectEagerly()
    private val runCommand: RunCommand by injectEagerly()
    private val fakeTelemetryPublisher: FakeTelemetryPublisher by injectEagerly()

    override fun additionalModules() = listOf(
        FakeRuntimeModule().module,
        FakeOperatingSystemModule().module
    )

    init {
        test("should record telemetry for successful command") {
            listRecipesCommand.test()

            val telemetry = fakeTelemetryPublisher.recordedTelemetries.firstOrNull()
            telemetry.shouldNotBeNull()
            telemetry.command shouldBe "ls"
            telemetry.recipes shouldHaveSize 0
            telemetry.executionTime shouldBeGreaterThan Duration.ZERO
            telemetry.outcome shouldBe SUCCESS
            telemetry.failure shouldBe null
            telemetry.git?.branch shouldBe FakeGitMetadata.branch
            telemetry.git?.repoOwner shouldBe FakeGitMetadata.repo.owner
            telemetry.git?.repoName shouldBe FakeGitMetadata.repo.name
            telemetry.os.name shouldBe FakeSystemInfo.os.name
            telemetry.os.version shouldBe FakeSystemInfo.os.version
            telemetry.os.arch shouldBe FakeSystemInfo.os.arch
            telemetry.os.username shouldBe FakeSystemInfo.os.username
            telemetry.context shouldHaveSize 2
            telemetry.context shouldBe mapOf("firstTestVariable" to "firstTestValue", "secondTestVariable" to "secondTestValue")
            telemetry.timestamp shouldBe Instant.parse("2024-07-24T10:00:00Z")
        }

        test("should record telemetry for successful command with recipes in parameters") {
            runRecipeCommand.test("--recipe pl.allegro.tech.allwrite.recipes.spring-boot-3")

            val telemetry = fakeTelemetryPublisher.recordedTelemetries.firstOrNull()
            telemetry.shouldNotBeNull()
            telemetry.command shouldBe "run-recipe"
            telemetry.recipes shouldHaveSize 1
            telemetry.recipes shouldBe listOf("pl.allegro.tech.allwrite.recipes.spring-boot-3")
            telemetry.executionTime shouldBeGreaterThan Duration.ZERO
            telemetry.outcome shouldBe SUCCESS
            telemetry.failure shouldBe null
            telemetry.git?.branch shouldBe FakeGitMetadata.branch
            telemetry.git?.repoOwner shouldBe FakeGitMetadata.repo.owner
            telemetry.git?.repoName shouldBe FakeGitMetadata.repo.name
            telemetry.os.name shouldBe FakeSystemInfo.os.name
            telemetry.os.version shouldBe FakeSystemInfo.os.version
            telemetry.os.arch shouldBe FakeSystemInfo.os.arch
            telemetry.os.username shouldBe FakeSystemInfo.os.username
            telemetry.context shouldHaveSize 2
            telemetry.context shouldBe mapOf("firstTestVariable" to "firstTestValue", "secondTestVariable" to "secondTestValue")
            telemetry.timestamp shouldBe Instant.parse("2024-07-24T10:00:00Z")
        }

        test("should record telemetry for successful command with recipes in arguments") {
            runCommand.test("spring-boot/upgrade", "2", "3")

            val telemetry = fakeTelemetryPublisher.recordedTelemetries.firstOrNull()
            telemetry.shouldNotBeNull()
            telemetry.command shouldBe "run"
            telemetry.recipes shouldHaveSize 1
            telemetry.recipes shouldBe listOf("pl.allegro.tech.allwrite.recipes.spring-boot-3")
            telemetry.executionTime shouldBeGreaterThan Duration.ZERO
            telemetry.outcome shouldBe SUCCESS
            telemetry.failure shouldBe null
            telemetry.git?.branch shouldBe FakeGitMetadata.branch
            telemetry.git?.repoOwner shouldBe FakeGitMetadata.repo.owner
            telemetry.git?.repoName shouldBe FakeGitMetadata.repo.name
            telemetry.os.name shouldBe FakeSystemInfo.os.name
            telemetry.os.version shouldBe FakeSystemInfo.os.version
            telemetry.os.arch shouldBe FakeSystemInfo.os.arch
            telemetry.os.username shouldBe FakeSystemInfo.os.username
            telemetry.context shouldHaveSize 2
            telemetry.context shouldBe mapOf("firstTestVariable" to "firstTestValue", "secondTestVariable" to "secondTestValue")
            telemetry.timestamp shouldBe Instant.parse("2024-07-24T10:00:00Z")
        }

        test("should record telemetry for successful command with recipes in file") {
            runRecipeCommand.test("--file src/test/resources/recipes.json")

            val telemetry = fakeTelemetryPublisher.recordedTelemetries.firstOrNull()
            telemetry.shouldNotBeNull()
            telemetry.command shouldBe "run-recipe"
            telemetry.recipes shouldHaveSize 2
            telemetry.recipes shouldBe listOf("pl.allegro.tech.allwrite.recipes.spring-boot-3", "pl.allegro.tech.allwrite.recipes.spring-boot-4")
            telemetry.executionTime shouldBeGreaterThan Duration.ZERO
            telemetry.outcome shouldBe SUCCESS
            telemetry.failure shouldBe null
            telemetry.git?.branch shouldBe FakeGitMetadata.branch
            telemetry.git?.repoOwner shouldBe FakeGitMetadata.repo.owner
            telemetry.git?.repoName shouldBe FakeGitMetadata.repo.name
            telemetry.os.name shouldBe FakeSystemInfo.os.name
            telemetry.os.version shouldBe FakeSystemInfo.os.version
            telemetry.os.arch shouldBe FakeSystemInfo.os.arch
            telemetry.os.username shouldBe FakeSystemInfo.os.username
            telemetry.context shouldHaveSize 2
            telemetry.context shouldBe mapOf("firstTestVariable" to "firstTestValue", "secondTestVariable" to "secondTestValue")
            telemetry.timestamp shouldBe Instant.parse("2024-07-24T10:00:00Z")
        }

        test("should not record telemetry for failed command execution") {
            runRecipeCommand.test("--incorrect parameter")

            val telemetry = fakeTelemetryPublisher.recordedTelemetries.firstOrNull()
            telemetry shouldBe null
        }
    }
}

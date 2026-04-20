package pl.allegro.tech.allwrite.cli

import com.github.ajalt.clikt.testing.test
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.cli.application.ListRecipesCommand
import pl.allegro.tech.allwrite.cli.application.RunCommand
import pl.allegro.tech.allwrite.cli.application.port.outgoing.Telemetry
import pl.allegro.tech.allwrite.cli.base.BaseCliSpec
import pl.allegro.tech.allwrite.cli.fake.external.FakeTelemetryPublisher
import pl.allegro.tech.allwrite.cli.fake.os.FakeGitMetadata
import pl.allegro.tech.allwrite.cli.fake.os.FakeOperatingSystemModule
import pl.allegro.tech.allwrite.cli.fake.os.FakeSystemInfo
import pl.allegro.tech.allwrite.runtime.fake.FakeRuntimeModule
import pl.allegro.tech.allwrite.runtime.util.injectEagerly
import java.time.Instant
import kotlin.time.Duration

class TelemetryRecordingSpec : BaseCliSpec() {

    private val listRecipesCommand: ListRecipesCommand by injectEagerly()
    private val runCommand: RunCommand by injectEagerly()
    private val fakeTelemetryPublisher: FakeTelemetryPublisher by injectEagerly()

    override fun additionalModules() =
        listOf(
            FakeRuntimeModule().module,
            FakeOperatingSystemModule().module,
        )

    init {
        test("should record telemetry for successful command") {
            listRecipesCommand.test()

            val telemetry = fakeTelemetryPublisher.recordedTelemetries.firstOrNull()
            telemetry.shouldNotBeNull()
            telemetry.command shouldBe "ls"
            telemetry.recipes shouldHaveSize 0
            telemetry.executionTime shouldBeGreaterThan Duration.Companion.ZERO
            telemetry.outcome shouldBe Telemetry.CommandOutcome.SUCCESS
            telemetry.failure shouldBe null
            telemetry.git?.branch shouldBe FakeGitMetadata.Companion.branch
            telemetry.git?.repoOwner shouldBe FakeGitMetadata.Companion.repo.owner
            telemetry.git?.repoName shouldBe FakeGitMetadata.Companion.repo.name
            telemetry.os.name shouldBe FakeSystemInfo.Companion.os.name
            telemetry.os.version shouldBe FakeSystemInfo.Companion.os.version
            telemetry.os.arch shouldBe FakeSystemInfo.Companion.os.arch
            telemetry.os.username shouldBe FakeSystemInfo.Companion.os.username
            telemetry.context shouldHaveSize 2
            telemetry.context shouldBe mapOf("firstTestVariable" to "firstTestValue", "secondTestVariable" to "secondTestValue")
            telemetry.timestamp shouldBe Instant.parse("2024-07-24T10:00:00Z")
        }

        test("should record telemetry for successful command with recipes in parameters") {
            runCommand.test("--recipe pl.allegro.tech.allwrite.recipes.spring-boot-3")

            val telemetry = fakeTelemetryPublisher.recordedTelemetries.firstOrNull()
            telemetry.shouldNotBeNull()
            telemetry.command shouldBe "run"
            telemetry.recipes shouldHaveSize 1
            telemetry.recipes shouldBe listOf("pl.allegro.tech.allwrite.recipes.spring-boot-3")
            telemetry.executionTime shouldBeGreaterThan Duration.Companion.ZERO
            telemetry.outcome shouldBe Telemetry.CommandOutcome.SUCCESS
            telemetry.failure shouldBe null
            telemetry.git?.branch shouldBe FakeGitMetadata.Companion.branch
            telemetry.git?.repoOwner shouldBe FakeGitMetadata.Companion.repo.owner
            telemetry.git?.repoName shouldBe FakeGitMetadata.Companion.repo.name
            telemetry.os.name shouldBe FakeSystemInfo.Companion.os.name
            telemetry.os.version shouldBe FakeSystemInfo.Companion.os.version
            telemetry.os.arch shouldBe FakeSystemInfo.Companion.os.arch
            telemetry.os.username shouldBe FakeSystemInfo.Companion.os.username
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
            telemetry.executionTime shouldBeGreaterThan Duration.Companion.ZERO
            telemetry.outcome shouldBe Telemetry.CommandOutcome.SUCCESS
            telemetry.failure shouldBe null
            telemetry.git?.branch shouldBe FakeGitMetadata.Companion.branch
            telemetry.git?.repoOwner shouldBe FakeGitMetadata.Companion.repo.owner
            telemetry.git?.repoName shouldBe FakeGitMetadata.Companion.repo.name
            telemetry.os.name shouldBe FakeSystemInfo.Companion.os.name
            telemetry.os.version shouldBe FakeSystemInfo.Companion.os.version
            telemetry.os.arch shouldBe FakeSystemInfo.Companion.os.arch
            telemetry.os.username shouldBe FakeSystemInfo.Companion.os.username
            telemetry.context shouldHaveSize 2
            telemetry.context shouldBe mapOf("firstTestVariable" to "firstTestValue", "secondTestVariable" to "secondTestValue")
            telemetry.timestamp shouldBe Instant.parse("2024-07-24T10:00:00Z")
        }

        test("should record telemetry for successful command with recipes in file") {
            runCommand.test("--file src/test/resources/recipes.json")

            val telemetry = fakeTelemetryPublisher.recordedTelemetries.firstOrNull()
            telemetry.shouldNotBeNull()
            telemetry.command shouldBe "run"
            telemetry.recipes shouldHaveSize 2
            telemetry.recipes shouldBe listOf("pl.allegro.tech.allwrite.recipes.spring-boot-3", "pl.allegro.tech.allwrite.recipes.spring-boot-4")
            telemetry.executionTime shouldBeGreaterThan Duration.Companion.ZERO
            telemetry.outcome shouldBe Telemetry.CommandOutcome.SUCCESS
            telemetry.failure shouldBe null
            telemetry.git?.branch shouldBe FakeGitMetadata.Companion.branch
            telemetry.git?.repoOwner shouldBe FakeGitMetadata.Companion.repo.owner
            telemetry.git?.repoName shouldBe FakeGitMetadata.Companion.repo.name
            telemetry.os.name shouldBe FakeSystemInfo.Companion.os.name
            telemetry.os.version shouldBe FakeSystemInfo.Companion.os.version
            telemetry.os.arch shouldBe FakeSystemInfo.Companion.os.arch
            telemetry.os.username shouldBe FakeSystemInfo.Companion.os.username
            telemetry.context shouldHaveSize 2
            telemetry.context shouldBe mapOf("firstTestVariable" to "firstTestValue", "secondTestVariable" to "secondTestValue")
            telemetry.timestamp shouldBe Instant.parse("2024-07-24T10:00:00Z")
        }

        test("should not record telemetry for failed command execution") {
            runCommand.test("--incorrect parameter")

            val telemetry = fakeTelemetryPublisher.recordedTelemetries.firstOrNull()
            telemetry shouldBe null
        }
    }
}

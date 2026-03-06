package pl.allegro.tech.allwrite.runner.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.runner.application.port.outgoing.AdditionalContextProvider
import pl.allegro.tech.allwrite.runner.application.port.outgoing.GitMetadata
import pl.allegro.tech.allwrite.runner.application.port.outgoing.JobContext
import pl.allegro.tech.allwrite.runner.application.port.outgoing.SystemInfo
import pl.allegro.tech.allwrite.runner.application.port.outgoing.Telemetry
import pl.allegro.tech.allwrite.runner.application.port.outgoing.Telemetry.CommandOutcome.FAILURE
import pl.allegro.tech.allwrite.runner.application.port.outgoing.Telemetry.CommandOutcome.SUCCESS
import pl.allegro.tech.allwrite.runner.application.port.outgoing.TelemetryPublisher
import java.time.Clock
import java.time.Instant

@Single
internal class TelemetryRecordingCommandListener(
    private val telemetryPublisher: TelemetryPublisher,
    private val gitMetadata: GitMetadata,
    private val systemInfo: SystemInfo,
    private val additionalContextProvider: AdditionalContextProvider,
    private val jobContext: JobContext,
    private val clock: Clock
) : CommandListener {

    override fun onCommandExecuted(event: CommandExecutedEvent) {
        try {
            recordTelemetryFor(event)
        } catch (e: Throwable) {
            if (logger.isDebugEnabled()) {
                logger.warn(e) { "Unable to record telemetry" }
            } else {
                logger.warn { "Unable to record telemetry" }
            }
        }
    }

    private fun recordTelemetryFor(event: CommandExecutedEvent) {
        val telemetry = Telemetry(
            command = event.command,
            recipes = event.recipes,
            executionTime = event.executionTime,
            outcome = if (event.throwable == null) SUCCESS else FAILURE,
            failure = event.throwable?.let(Telemetry::Failure),
            git = Telemetry.Git(
                repoOwner = gitMetadata.repo.owner,
                repoName = gitMetadata.repo.name,
                branch = gitMetadata.branch,
            ),
            os = Telemetry.OperatingSystem(
                name = systemInfo.os.name,
                version = systemInfo.os.version,
                arch = systemInfo.os.arch,
                username = systemInfo.os.username
            ),
            hardware = Telemetry.Hardware(
                cpus = systemInfo.cpu.cores,
                memoryUsed = systemInfo.memory.used,
                memoryTotal = systemInfo.memory.total,
            ),
            context = additionalContextProvider.extractFromSystemEnvs(),
            timestamp = Instant.now(clock),
            jobUrl = jobContext.jobUrl
        )
        telemetryPublisher.publishTelemetry(telemetry)
    }

    companion object {

        private val logger = KotlinLogging.logger {}
    }
}

package pl.allegro.tech.allwrite.cli.application.port.outgoing

import org.koin.core.annotation.Single
import java.time.Instant
import kotlin.time.Duration

public interface TelemetryPublisher {
    public fun publishTelemetry(telemetry: Telemetry)
}

@Single
internal class NoOpTelemetryPublisher : TelemetryPublisher {
    override fun publishTelemetry(telemetry: Telemetry) {
        // no-op
    }
}

public data class Telemetry(
    val command: String,
    val recipes: List<String>,
    val executionTime: Duration,
    val outcome: CommandOutcome,
    val failure: Failure?,
    val git: Git?,
    val os: OperatingSystem,
    val hardware: Hardware,
    val context: Map<String, String>,
    val timestamp: Instant,
    val jobUrl: String?,
) {
    public enum class CommandOutcome {
        SUCCESS,
        FAILURE,
    }

    public data class Failure(
        val exceptionName: String?,
        val messageMessage: String?,
    ) {
        public constructor(throwable: Throwable) :
            this(throwable.javaClass.simpleName, throwable.message)
    }

    public data class Git(
        val branch: String,
        val repoOwner: String,
        val repoName: String,
    )

    public data class OperatingSystem(
        val name: String,
        val version: String,
        val arch: String,
        val username: String,
    )

    public data class Hardware(
        val cpus: Int,
        val memoryUsed: Long,
        val memoryTotal: Long,
    )
}

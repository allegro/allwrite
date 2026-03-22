package pl.allegro.tech.allwrite.cli.fake.external

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.port.outgoing.Telemetry
import pl.allegro.tech.allwrite.cli.application.port.outgoing.TelemetryPublisher

@Single
class FakeTelemetryPublisher : TelemetryPublisher {

    val recordedTelemetries = mutableListOf<Telemetry>()

    override fun publishTelemetry(telemetry: Telemetry) {
        println("Recorded telemetry: $telemetry")
        recordedTelemetries.add(telemetry)
    }
}

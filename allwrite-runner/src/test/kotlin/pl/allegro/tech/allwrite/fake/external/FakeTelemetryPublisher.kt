package pl.allegro.tech.allwrite.fake.external

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.runner.application.port.outgoing.Telemetry
import pl.allegro.tech.allwrite.runner.application.port.outgoing.TelemetryPublisher

@Single
class FakeTelemetryPublisher : TelemetryPublisher {

    val recordedTelemetries = mutableListOf<Telemetry>()

    override fun publishTelemetry(telemetry: Telemetry) {
        println("Recorded telemetry: $telemetry")
        recordedTelemetries.add(telemetry)
    }
}

package pl.allegro.tech.allwrite.cli.fake.clock

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Module
@ComponentScan
class FakeClockModule {

    @Single
    fun clock(): Clock = Clock.fixed(Instant.parse("2024-07-24T10:00:00Z"), ZoneId.of("Europe/Warsaw"))
}

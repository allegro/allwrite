package pl.allegro.tech.allwrite.cli.util

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import java.time.Clock

@Module
@ComponentScan
public class ClockModule {

    @Single
    internal fun clock(): Clock = Clock.systemUTC()
}

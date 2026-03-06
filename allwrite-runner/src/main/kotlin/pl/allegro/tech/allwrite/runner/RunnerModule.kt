package pl.allegro.tech.allwrite.runner

import org.koin.core.annotation.Module
import pl.allegro.tech.allwrite.common.RuntimeModule
import pl.allegro.tech.allwrite.runner.application.ApplicationModule
import pl.allegro.tech.allwrite.runner.infrastructure.os.OperatingSystemModule
import pl.allegro.tech.allwrite.runner.infrastructure.pullrequestmanager.PullRequestManagerModule
import pl.allegro.tech.allwrite.runner.util.ClockModule

/**
 * This module aggregates all core modules required for the runner to work correctly.
 *
 * NOTE: there is no @ComponentScan here to allow some modules to be conditionally loaded.
 */
@Module(includes = [
    ApplicationModule::class,
    RuntimeModule::class,
    OperatingSystemModule::class,
    ClockModule::class,
    PullRequestManagerModule::class,
])
public class RunnerModule

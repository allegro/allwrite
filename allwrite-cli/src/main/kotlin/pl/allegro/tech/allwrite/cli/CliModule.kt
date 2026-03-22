package pl.allegro.tech.allwrite.cli

import org.koin.core.annotation.Module
import pl.allegro.tech.allwrite.common.RuntimeModule
import pl.allegro.tech.allwrite.cli.application.ApplicationModule
import pl.allegro.tech.allwrite.cli.infrastructure.os.OperatingSystemModule
import pl.allegro.tech.allwrite.cli.infrastructure.pullrequestmanager.PullRequestManagerModule
import pl.allegro.tech.allwrite.cli.util.ClockModule

/**
 * This module aggregates all core modules required for the CLI to work correctly.
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
public class CliModule

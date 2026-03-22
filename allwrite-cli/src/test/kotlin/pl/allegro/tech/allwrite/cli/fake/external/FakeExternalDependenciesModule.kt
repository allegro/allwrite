package pl.allegro.tech.allwrite.cli.fake.external

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/**
 * Fakes all external dependencies to avoid sending requests to real services.
 *
 * The beans faked by this module can span multiple production modules.
 */
@Module
@ComponentScan
class FakeExternalDependenciesModule

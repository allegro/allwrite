package pl.allegro.tech.allwrite.cli.base

import io.kotest.core.spec.style.FunSpec
import org.koin.core.module.Module
import org.koin.ksp.generated.module
import org.koin.test.KoinTest
import pl.allegro.tech.allwrite.cli.CliModule
import pl.allegro.tech.allwrite.cli.fake.clock.FakeClockModule
import pl.allegro.tech.allwrite.cli.fake.external.FakeExternalDependenciesModule
import pl.allegro.tech.allwrite.runtime.util.KoinMockkExtension

abstract class BaseCliSpec :
    FunSpec(),
    KoinTest {

    override fun extensions() =
        listOf(
            KoinMockkExtension(
                CliModule().module,
                FakeExternalDependenciesModule().module, // always faked to avoid sending requests to real services
                FakeClockModule().module,
                *additionalModules().toTypedArray(),
            ),
        )

    protected open fun additionalModules(): List<Module> = emptyList()
}

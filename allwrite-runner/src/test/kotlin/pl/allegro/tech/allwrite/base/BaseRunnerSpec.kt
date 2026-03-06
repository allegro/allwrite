package pl.allegro.tech.allwrite.base

import io.kotest.core.spec.style.FunSpec
import org.koin.core.module.Module
import org.koin.ksp.generated.module
import org.koin.test.KoinTest
import pl.allegro.tech.allwrite.common.util.KoinMockkExtension
import pl.allegro.tech.allwrite.fake.clock.FakeClockModule
import pl.allegro.tech.allwrite.fake.external.FakeExternalDependenciesModule
import pl.allegro.tech.allwrite.runner.RunnerModule

abstract class BaseRunnerSpec : FunSpec(), KoinTest {

    override fun extensions() = listOf(
        KoinMockkExtension(
            RunnerModule().module,
            FakeExternalDependenciesModule().module, // always faked to avoid sending requests to real services
            FakeClockModule().module,
            *additionalModules().toTypedArray()
        )
    )

    protected open fun additionalModules(): List<Module> = emptyList()
}

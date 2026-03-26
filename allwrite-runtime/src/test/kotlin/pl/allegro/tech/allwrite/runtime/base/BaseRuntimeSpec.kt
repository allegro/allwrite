package pl.allegro.tech.allwrite.runtime.base

import io.kotest.core.spec.style.FunSpec
import org.koin.core.module.Module
import org.koin.ksp.generated.module
import org.koin.test.KoinTest
import pl.allegro.tech.allwrite.runtime.RuntimeModule
import pl.allegro.tech.allwrite.runtime.port.outgoing.fake.FakeRuntimeOutgoingPortsModule
import pl.allegro.tech.allwrite.runtime.util.KoinMockkExtension

abstract class BaseRuntimeSpec : FunSpec(), KoinTest {
    override fun extensions() = listOf(
        KoinMockkExtension(
            RuntimeModule().module,
            FakeRuntimeOutgoingPortsModule().module,
            *additionalModules().toTypedArray()
        )
    )

    protected open fun additionalModules(): List<Module> = emptyList()
}

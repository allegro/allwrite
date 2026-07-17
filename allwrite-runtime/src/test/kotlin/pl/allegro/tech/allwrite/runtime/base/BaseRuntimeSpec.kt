package pl.allegro.tech.allwrite.runtime.base

import io.kotest.core.spec.style.FunSpec
import org.koin.core.module.Module
import org.koin.test.KoinTest
import pl.allegro.tech.allwrite.runtime.util.KoinMockkExtension

abstract class BaseRuntimeSpec :
    FunSpec(),
    KoinTest {
    override fun extensions() =
        listOf(
            KoinMockkExtension(
                RuntimeTestModules.runtime,
                RuntimeTestModules.fakeOutgoingPorts,
                *additionalModules().toTypedArray(),
            ),
        )

    protected open fun additionalModules(): List<Module> = emptyList()
}

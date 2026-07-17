package pl.allegro.tech.allwrite.cli.base

import io.kotest.core.spec.style.FunSpec
import org.koin.core.module.Module
import org.koin.test.KoinTest
import pl.allegro.tech.allwrite.cli.TestModules
import pl.allegro.tech.allwrite.runtime.util.KoinMockkExtension

abstract class BaseCliSpec :
    FunSpec(),
    KoinTest {

    override fun extensions() =
        listOf(
            KoinMockkExtension(
                TestModules.cli,
                TestModules.external,
                TestModules.clock,
                *additionalModules().toTypedArray(),
            ),
        )

    protected open fun additionalModules(): List<Module> = emptyList()
}

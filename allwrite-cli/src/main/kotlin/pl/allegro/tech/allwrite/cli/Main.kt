package pl.allegro.tech.allwrite.cli

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.kapt.GenerateCompletions
import pl.allegro.tech.allwrite.cli.application.port.incoming.AppEntrypoint
import pl.allegro.tech.allwrite.cli.infrastructure.bot.GithubBotModule
import pl.allegro.tech.allwrite.cli.infrastructure.github.GithubModule
import pl.allegro.tech.allwrite.cli.infrastructure.os.port.incoming.SystemEnvironment

@GenerateCompletions
public fun main(args: Array<String>) {
    startKoin {
        modules(CliModule().module)
        loadDynamicModules()
        koin.get<AppEntrypoint>().execute(args)
    }
}

private fun KoinApplication.loadDynamicModules() {
    val dynamicModules = mapOf(
        "GITHUB_ACTIONS" to GithubModule().module,
        "GH_BOT" to GithubBotModule().module
    )
    dynamicModules
        .filterKeys { env[it] == "true" }
        .values
        .forEach(::modules)
}

private val KoinApplication.env: SystemEnvironment
    get() = koin.get<SystemEnvironment>()

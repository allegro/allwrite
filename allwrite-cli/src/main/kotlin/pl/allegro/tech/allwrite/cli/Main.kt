package pl.allegro.tech.allwrite.cli

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.kapt.GenerateCompletions
import pl.allegro.tech.allwrite.cli.application.port.incoming.AppEntrypoint
import pl.allegro.tech.allwrite.cli.infrastructure.github.GithubModule
import pl.allegro.tech.allwrite.cli.infrastructure.os.port.incoming.SystemEnvironment

@GenerateCompletions
public fun main(args: Array<String>) {
    startKoin {
        modules(CliModule().module)

        // todo: migrate to a custom variable PULL_REQUEST_MANAGER=true
        if (env["GITHUB_ACTIONS"] == "true") {
            modules(GithubModule().module)
        }

        koin.get<AppEntrypoint>().execute(args)
    }
}

private val KoinApplication.env: SystemEnvironment
    get() = koin.get<SystemEnvironment>()

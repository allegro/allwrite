package pl.allegro.tech.allwrite.runner

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.kapt.GenerateCompletions
import pl.allegro.tech.allwrite.runner.application.port.incoming.AppEntrypoint
import pl.allegro.tech.allwrite.runner.infrastructure.github.GithubModule
import pl.allegro.tech.allwrite.runner.infrastructure.os.port.incoming.SystemEnvironment

@GenerateCompletions
public fun main(args: Array<String>) {
    startKoin {
        modules(RunnerModule().module)

        // todo: migrate to a custom variable PULL_REQUEST_MANAGER=true
        if (env["GITHUB_ACTIONS"] == "true") {
            modules(GithubModule().module)
        }

        koin.get<AppEntrypoint>().execute(args)
    }
}

private val KoinApplication.env: SystemEnvironment
    get() = koin.get<SystemEnvironment>()

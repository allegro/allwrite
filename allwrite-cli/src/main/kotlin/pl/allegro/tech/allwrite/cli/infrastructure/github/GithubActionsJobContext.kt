package pl.allegro.tech.allwrite.cli.infrastructure.github

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.port.outgoing.JobContext
import pl.allegro.tech.allwrite.cli.infrastructure.os.port.incoming.SystemEnvironment

@Single
internal class GithubActionsJobContext(
    environment: SystemEnvironment,
) : JobContext {

    override val jobUrl: String? = environment["GITHUB_JOB_URL"]
}

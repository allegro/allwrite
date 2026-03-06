package pl.allegro.tech.allwrite.runner.infrastructure.github

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.runner.application.port.outgoing.GitMetadata
import pl.allegro.tech.allwrite.runner.application.port.outgoing.GitRepo
import pl.allegro.tech.allwrite.runner.infrastructure.os.OperatingSystemModule
import pl.allegro.tech.allwrite.runner.infrastructure.os.port.incoming.SystemEnvironment

/**
 * When executing within GitHub workflow, this bean will override [GitMetadata] implementation
 * from [OperatingSystemModule]
 */
@Single
internal class GithubActionsGitMetadata(
    private val environment: SystemEnvironment
) : GitMetadata {

    override val branch: String by lazy {
        environment.requireWithFallbacks("GITHUB_HEAD_REF", "GITHUB_REF_NAME")
    }

    override val repo: GitRepo by lazy {
        GitRepo(
            owner = environment.require("GITHUB_REPOSITORY_OWNER"),
            name = environment.require("GITHUB_REPOSITORY").substringAfter("/")
        )
    }
}

package pl.allegro.tech.allwrite.fake.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.runner.application.port.outgoing.GitMetadata
import pl.allegro.tech.allwrite.runner.application.port.outgoing.GitRepo

@Single
open class FakeGitMetadata : GitMetadata {

    override val branch = "test-branch"

    override val repo = GitRepo(
        owner = "test-org",
        name = "test-repo"
    )

    companion object : FakeGitMetadata()
}

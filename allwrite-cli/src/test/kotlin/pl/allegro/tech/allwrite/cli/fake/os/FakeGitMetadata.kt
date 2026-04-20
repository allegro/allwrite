package pl.allegro.tech.allwrite.cli.fake.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.port.outgoing.GitMetadata
import pl.allegro.tech.allwrite.cli.application.port.outgoing.GitRepo

@Single
open class FakeGitMetadata : GitMetadata {

    override val branch = "test-branch"

    override val repo = GitRepo(
        owner = "test-org",
        name = "test-repo",
    )

    companion object : FakeGitMetadata()
}

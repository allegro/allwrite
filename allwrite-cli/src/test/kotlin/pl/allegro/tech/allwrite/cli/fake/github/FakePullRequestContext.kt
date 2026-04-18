package pl.allegro.tech.allwrite.cli.fake.github

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.port.outgoing.PullRequestContext

@Single
class FakePullRequestContext : PullRequestContext {

    private var description: String = ORIGINAL_DESCRIPTION

    override fun getDescription(): String = description

    override fun updateDescription(description: String) {
        this.description = description
    }

    internal companion object {
        const val ORIGINAL_DESCRIPTION = "original description"
    }
}

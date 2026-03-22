package pl.allegro.tech.allwrite.cli.infrastructure.github

import com.spotify.github.v3.clients.PullRequestClient
import com.spotify.github.v3.prs.requests.ImmutablePullRequestUpdate
import pl.allegro.tech.allwrite.cli.application.port.outgoing.PullRequestContext
import kotlin.jvm.optionals.getOrNull

public class GithubPullRequestContext(
    private val client: PullRequestClient,
    private val pullRequestNumber: Long,
) : PullRequestContext {
    override fun getDescription(): String? = client.get(pullRequestNumber).get().body().getOrNull()

    override fun updateDescription(description: String) {
        client.update(pullRequestNumber, ImmutablePullRequestUpdate.builder().body(description).build())
    }
}

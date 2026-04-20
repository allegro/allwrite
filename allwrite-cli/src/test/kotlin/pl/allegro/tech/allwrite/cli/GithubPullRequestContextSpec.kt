package pl.allegro.tech.allwrite.cli

import com.spotify.github.v3.clients.PullRequestClient
import com.spotify.github.v3.prs.PullRequest
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pl.allegro.tech.allwrite.cli.infrastructure.github.GithubPullRequestContext
import java.util.Optional
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull

class GithubPullRequestContextSpec : FunSpec() {

    init {
        val client = mockk<PullRequestClient>()

        test("should use github client to get description") {
            // given
            val prNumber = 123L
            val prDescription = "test description"
            val context = GithubPullRequestContext(client, prNumber)
            val pr = mockk<PullRequest> {
                every { body() } returns Optional.of(prDescription)
            }
            every { client.get(prNumber) } returns CompletableFuture.completedFuture(pr)

            // when
            context.getDescription()

            // then
            verify {
                client.get(123L)
            }
        }

        test("should use github client to update description") {
            // given
            val prNumber = 123L
            val prDescription = "test description"
            val newDescription = "new description"
            val context = GithubPullRequestContext(client, prNumber)
            val pr = mockk<PullRequest>()
            every { client.update(prNumber, any()) } returns CompletableFuture.completedFuture(pr)

            // when
            context.updateDescription(newDescription)

            // then
            verify {
                client.update(
                    123L,
                    match {
                        it.body().getOrNull() == newDescription &&
                            it.title().isEmpty &&
                            it.state().isEmpty
                    },
                )
            }
        }
    }
}

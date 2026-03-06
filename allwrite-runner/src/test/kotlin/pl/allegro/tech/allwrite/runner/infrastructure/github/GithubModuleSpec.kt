package pl.allegro.tech.allwrite.runner.infrastructure.github

import com.spotify.github.v3.clients.GitHubClient
import com.spotify.github.v3.clients.PullRequestClient
import com.spotify.github.v3.clients.RepositoryClient
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.koin.ksp.generated.module
import org.koin.test.mock.declareMock
import pl.allegro.tech.allwrite.base.BaseRunnerSpec
import pl.allegro.tech.allwrite.common.util.declareFake
import pl.allegro.tech.allwrite.common.util.injectEagerly
import pl.allegro.tech.allwrite.runner.application.port.outgoing.NoPullRequestContext
import pl.allegro.tech.allwrite.runner.application.port.outgoing.PullRequestContext
import pl.allegro.tech.allwrite.runner.infrastructure.github.GithubPullRequestContext
import pl.allegro.tech.allwrite.runner.infrastructure.os.port.incoming.SystemEnvironment

class GithubModuleSpec : BaseRunnerSpec() {

    private val pullRequestContext: PullRequestContext by injectEagerly()

    override fun additionalModules() = listOf(
        GithubModule().module
    )

    init {

        beforeEach {
            declareMock<GitHubClient> {
                val pullRequestClientMock = mockk<PullRequestClient>()
                val repositoryClientMock = mockk<RepositoryClient> { every { createPullRequestClient() } returns pullRequestClientMock }
                every { createRepositoryClient(any(), any()) } returns repositoryClientMock
            }
        }

        test("should create NoPullRequestContext when REWRITE_REPOSITORY is null") {
            // given
            declareFake<SystemEnvironment>(GithubPullRequestContextEnvironment(repository = null))

            // expect
            pullRequestContext.shouldBeInstanceOf<NoPullRequestContext>()
        }

        test("should create NoPullRequestContext when REWRITE_PR_NUMBER is null") {
            // given
            declareFake<SystemEnvironment>(GithubPullRequestContextEnvironment(pullRequestNumber = null))

            // expect
            pullRequestContext.shouldBeInstanceOf<NoPullRequestContext>()
        }

        test("should create NoPullRequestContext when REWRITE_REPOSITORY is invalid") {
            // given
            declareFake<SystemEnvironment>(GithubPullRequestContextEnvironment(repository = "org expected"))

            // expect
            pullRequestContext.shouldBeInstanceOf<NoPullRequestContext>()
        }

        test("should create NoPullRequestContext when REWRITE_PR_NUMBER is invalid") {
            // given
            declareFake<SystemEnvironment>(GithubPullRequestContextEnvironment(pullRequestNumber = "not a number"))

            // expect
            pullRequestContext.shouldBeInstanceOf<NoPullRequestContext>()
        }


        test("should create GithubPullRequestContext to get description") {
            // given
            declareFake<SystemEnvironment>(GithubPullRequestContextEnvironment())

            // expect
            pullRequestContext.shouldBeInstanceOf<GithubPullRequestContext>()
        }
    }

    private class GithubPullRequestContextEnvironment(
        val repository: String? = "example-org/example",
        val pullRequestNumber: String? = "123",
    ) : SystemEnvironment {

        private val source : Map<String, String> = buildMap {
            if (repository != null) {
                put("REWRITE_REPOSITORY", repository)
            }

            if (pullRequestNumber != null) {
                put("REWRITE_PR_NUMBER", pullRequestNumber)
            }
        }

        override fun get(name: String): String? = source[name]
    }
}

package pl.allegro.tech.allwrite.runner.infrastructure.github

import com.spotify.github.v3.clients.GitHubClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.runner.application.port.outgoing.NoPullRequestContext
import pl.allegro.tech.allwrite.runner.application.port.outgoing.PullRequestContext
import pl.allegro.tech.allwrite.runner.infrastructure.github.GithubPullRequestContext
import pl.allegro.tech.allwrite.runner.infrastructure.os.port.incoming.SystemEnvironment
import java.net.URI

/**
 * This module provides interaction with GitHub and takes advantage of GitHub Actions context
 */
@Module
@ComponentScan
public class GithubModule {
    @Single
    internal fun githubClient() =
        GitHubClient.create(URI.create("https://api.github.com/"), System.getenv("GITHUB_TOKEN"))

    @Single
    internal fun pullRequestContext(
        gitHubClient: GitHubClient,
        systemEnvironment: SystemEnvironment,
    ): PullRequestContext {
        val envRepository = systemEnvironment["REWRITE_REPOSITORY"]
        val envPrNumber = systemEnvironment["REWRITE_PR_NUMBER"]
        if (envRepository == null || envPrNumber == null) return NoPullRequestContext()

        try {
            val (owner, repo) = envRepository.split("/", limit = 2)
            val pullRequestNumber = envPrNumber.toLong()
            val pullRequestClient = gitHubClient.createRepositoryClient(owner, repo).createPullRequestClient()
            return GithubPullRequestContext(pullRequestClient, pullRequestNumber)
        } catch (e: Exception) {
            logger.debug(e) { "Can't initialize pull request context" }
            return NoPullRequestContext()
        }
    }

    private companion object {
        val logger = KotlinLogging.logger {}
    }
}

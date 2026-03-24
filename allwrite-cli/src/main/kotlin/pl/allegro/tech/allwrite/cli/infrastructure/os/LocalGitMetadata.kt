package pl.allegro.tech.allwrite.cli.infrastructure.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.port.outgoing.GitMetadata
import pl.allegro.tech.allwrite.cli.application.port.outgoing.GitRepo

@Single
internal class LocalGitMetadata(
    private val commandExecutor: SystemCommandExecutor
) : GitMetadata {

    override val branch: String? by lazy {
        try {
            commandExecutor.exec("git branch --show-current")
                .takeIf { it.isNotBlank() }
                ?: "detached HEAD"
        } catch (_: Exception) {
            null
        }
    }

    override val repo: GitRepo? by lazy {
        try {
            val repoUrl = commandExecutor.exec("git remote get-url origin")
            val match = GIT_REMOTE_URL_REGEX.find(repoUrl) ?: error("$repoUrl not matching regex $GIT_REMOTE_URL_REGEX")
            GitRepo(
                owner = match.groupValues[1],
                name = match.groupValues[2],
            )
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val GIT_REMOTE_URL_REGEX = """.*github\.com[/:]([^/]+)/([^/.]+).*""".toRegex()
    }
}

package pl.allegro.tech.allwrite.runner.application.port.outgoing

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

public interface PullRequestContext {
    public fun getDescription(): String?
    public fun updateDescription(description: String)
}

@Single
internal class NoPullRequestContext : PullRequestContext {

    init {
        logger.debug { "No Pull Request Context available" }
    }

    override fun getDescription(): String? = null

    override fun updateDescription(description: String) {
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

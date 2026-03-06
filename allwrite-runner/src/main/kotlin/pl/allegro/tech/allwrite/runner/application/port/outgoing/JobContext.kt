package pl.allegro.tech.allwrite.runner.application.port.outgoing

import org.koin.core.annotation.Single

public interface JobContext {
    public val jobUrl: String?
}

@Single
internal class NoJobContext : JobContext {
    override val jobUrl: String? = null
}

package pl.allegro.tech.allwrite.runner.application.port.outgoing

public interface GitMetadata {
    public val branch: String
    public val repo: GitRepo
}

public data class GitRepo(
    val owner: String,
    val name: String
)

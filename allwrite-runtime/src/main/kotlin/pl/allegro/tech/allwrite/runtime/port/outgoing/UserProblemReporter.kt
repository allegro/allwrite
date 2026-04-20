package pl.allegro.tech.allwrite.runtime.port.outgoing

/**
 * It should be used for reporting user-facing, actionable, well formatted problem descriptions.
 * The implementation of this interface should present those problems directly to the user.
 */
public interface UserProblemReporter {
    public fun reportProblem(problem: Problem)
}

public data class Problem(
    val message: String,
)

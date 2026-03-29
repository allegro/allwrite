package pl.allegro.tech.allwrite.runtime.fake

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.runtime.port.outgoing.Problem
import pl.allegro.tech.allwrite.runtime.port.outgoing.UserProblemReporter

@Single
class FakeUserProblemReporter : UserProblemReporter {

    private val _reportedProblems: MutableList<Problem> = mutableListOf()
    val reportedProblems: List<Problem> = _reportedProblems

    override fun reportProblem(problem: Problem) {
        _reportedProblems.add(problem)
    }
}

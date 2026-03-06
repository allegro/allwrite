package pl.allegro.tech.allwrite

import io.kotest.matchers.shouldBe
import io.mockk.every
import org.koin.test.mock.declareMock
import pl.allegro.tech.allwrite.base.BaseRunnerSpec
import pl.allegro.tech.allwrite.common.util.injectEagerly
import pl.allegro.tech.allwrite.runner.infrastructure.os.LocalGitMetadata
import pl.allegro.tech.allwrite.runner.infrastructure.os.SystemCommandExecutor

class LocalGitMetadataSpec : BaseRunnerSpec() {

    private val localGitMetadata: LocalGitMetadata by injectEagerly()

    init {
        test("should parse git remote URL on local operating system via SSH") {
            declareMock<SystemCommandExecutor> {
                every { exec("git remote get-url origin") }
                    .returns("git@github.com:example-org/some-service.git")
            }

            localGitMetadata.repo.owner shouldBe "example-org"
            localGitMetadata.repo.name shouldBe "some-service"
        }

        test("should parse git remote URL on local operating system via HTTPS") {
            declareMock<SystemCommandExecutor> {
                every { exec("git remote get-url origin") }
                    .returns("https://github.com/example-org/some-service.git")
            }

            localGitMetadata.repo.owner shouldBe "example-org"
            localGitMetadata.repo.name shouldBe "some-service"
        }
    }
}

package pl.allegro.tech.allwrite.cli

import io.kotest.matchers.shouldBe
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.cli.application.port.incoming.AppEntrypoint
import pl.allegro.tech.allwrite.cli.base.BaseCliSpec
import pl.allegro.tech.allwrite.cli.fake.os.FakeOperatingSystemModule
import pl.allegro.tech.allwrite.cli.fake.os.FakeSystemEnvironment
import pl.allegro.tech.allwrite.cli.fake.recipes.FailingPostProcessingRecipe
import pl.allegro.tech.allwrite.common.util.injectEagerly
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.io.path.readText

/**
 * This test aims to use as few faked dependencies as possible.
 * Only OS integration is faked to allow easy setup for environment variables and predictable system-dependent values.
 */
class CliIntegrationSpec : BaseCliSpec() {

    private val appEntrypoint: AppEntrypoint by injectEagerly()
    private val fakeSystemEnvironment: FakeSystemEnvironment by injectEagerly()

    override fun additionalModules() = listOf(
        FakeOperatingSystemModule().module
    )

    init {
        test("should post a comment with summary from failing post-processing recipe") {
            // given
            val summaryCommentFile = createSummaryCommentFile()
            fakeSystemEnvironment["PR_MANAGER_SUMMARY_COMMENT_FILE"] = summaryCommentFile.pathString

            // when
            appEntrypoint.execute("run", "--recipe", FailingPostProcessingRecipe.name)

            // then
            summaryCommentFile.readText() shouldBe """
                Failed to automatically apply some parts of the migration:
                * Something went wrong

            """.trimIndent()
        }
    }

    private fun createSummaryCommentFile(): Path =
        Files.createTempFile("rewrite", "summary-comment")
}

private fun AppEntrypoint.execute(vararg args: String) {
    execute(args.toList().toTypedArray())
}

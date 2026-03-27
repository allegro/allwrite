package pl.allegro.tech.allwrite.cli.fake.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.infrastructure.os.AllwriteHome
import java.nio.file.Files
import java.nio.file.Path

@Single
internal class FakeAllwriteHome : AllwriteHome() {
    override val path: Path = Files.createTempDirectory("allwrite-test")
}

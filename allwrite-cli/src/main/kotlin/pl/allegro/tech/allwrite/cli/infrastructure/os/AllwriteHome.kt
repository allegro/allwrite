package pl.allegro.tech.allwrite.cli.infrastructure.os

import org.koin.core.annotation.Single
import java.nio.file.Path

@Single
internal open class AllwriteHome {
    open val path: Path = Path.of(System.getProperty("user.home"), ".config", "allwrite")
}

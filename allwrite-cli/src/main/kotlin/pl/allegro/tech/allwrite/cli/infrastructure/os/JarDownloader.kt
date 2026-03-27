package pl.allegro.tech.allwrite.cli.infrastructure.os

import java.nio.file.Path

internal interface JarDownloader {
    fun download(url: String, destination: Path)
}

package pl.allegro.tech.allwrite.cli.infrastructure.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.infrastructure.os.port.incoming.SystemEnvironment

@Single
internal class NonBlankSystemEnvironment : SystemEnvironment {

    override fun get(name: String): String? {
        val envVariable = System.getenv(name)?.notBlankOrNull()
        val systemProperty = System.getProperty(name)?.notBlankOrNull()
        return envVariable ?: systemProperty
    }
}

private fun String.notBlankOrNull(): String? = takeIf(String::isNotBlank)

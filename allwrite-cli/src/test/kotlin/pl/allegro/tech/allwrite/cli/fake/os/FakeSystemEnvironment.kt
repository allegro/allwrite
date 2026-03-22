package pl.allegro.tech.allwrite.cli.fake.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.infrastructure.os.port.incoming.SystemEnvironment

@Single
class FakeSystemEnvironment(
    private val env: MutableMap<String, String> = mutableMapOf()
) : SystemEnvironment, MutableMap<String, String> by env

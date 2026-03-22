package pl.allegro.tech.allwrite.cli.infrastructure.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.port.outgoing.OperatingSystemInfo
import pl.allegro.tech.allwrite.cli.application.port.outgoing.SystemCpuInfo
import pl.allegro.tech.allwrite.cli.infrastructure.os.port.incoming.SystemEnvironment
import pl.allegro.tech.allwrite.cli.application.port.outgoing.SystemInfo
import pl.allegro.tech.allwrite.cli.application.port.outgoing.SystemMemoryInfo

@Single
internal class JvmRuntimeSystemInfo(
    systemEnvironment: SystemEnvironment
) : SystemInfo {

    override val os = OperatingSystemInfo(
        name = systemEnvironment.require("os.name"),
        version = systemEnvironment.require("os.version"),
        arch = systemEnvironment.require("os.arch"),
        username = systemEnvironment.require("user.name"),
    )

    private val jvmRuntime = Runtime.getRuntime()

    override val cpu = SystemCpuInfo(
        cores = jvmRuntime.availableProcessors()
    )

    override val memory = SystemMemoryInfo(
        total = jvmRuntime.totalMemory(),
        free = jvmRuntime.freeMemory()
    )
}

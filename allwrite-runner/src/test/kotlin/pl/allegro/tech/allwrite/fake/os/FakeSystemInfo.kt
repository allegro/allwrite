package pl.allegro.tech.allwrite.fake.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.runner.application.port.outgoing.OperatingSystemInfo
import pl.allegro.tech.allwrite.runner.application.port.outgoing.SystemCpuInfo
import pl.allegro.tech.allwrite.runner.application.port.outgoing.SystemInfo
import pl.allegro.tech.allwrite.runner.application.port.outgoing.SystemMemoryInfo

@Single
open class FakeSystemInfo : SystemInfo {

    override val os = OperatingSystemInfo(
        name = "test-os",
        version = "10.01",
        arch = "aarch64",
        username = "user",
    )

    override val cpu = SystemCpuInfo(
        cores = 16
    )

    override val memory = SystemMemoryInfo(
        total = 1024,
        free = 256
    )

    companion object : FakeSystemInfo()
}

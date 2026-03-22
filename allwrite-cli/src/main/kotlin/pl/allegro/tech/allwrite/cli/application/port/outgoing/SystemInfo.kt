package pl.allegro.tech.allwrite.cli.application.port.outgoing

public interface SystemInfo {
    public val os: OperatingSystemInfo
    public val cpu: SystemCpuInfo
    public val memory: SystemMemoryInfo
}

public data class OperatingSystemInfo(
    public val name: String,
    public val version: String,
    public val arch: String,
    public val username: String
)

public data class SystemCpuInfo(
    val cores: Int
)

public data class SystemMemoryInfo(
    val total: Long,
    val free: Long,
) {
    val used: Long = total - free
}

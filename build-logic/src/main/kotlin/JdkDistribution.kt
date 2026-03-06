internal enum class JdkDistribution(
    /**
     * Name used to make up the Temurin JDK release asset filename
     */
    val temurinName: String,

    /**
     * Identifier of the JReleaser platform
     */
    val jreleaserName: String,

    /**
     * Name to be used in task names
     */
    val taskSuffix: String,

    /**
     * Path to the JAVA_HOME after JDK archive extraction
     */
    val jdkPath: String,

    /**
     * For example: 'tar.gz', 'zip'
     */
    val archiveFormat: String
) {
    OSX_AARCH64(
        temurinName = "aarch64_mac",
        jreleaserName = "osx-aarch_64",
        taskSuffix = "OsxAarch64",
        jdkPath = "jdk/Contents/Home",
        archiveFormat = "tar.gz"
    ),
    OSX_X64(
        temurinName = "x64_mac",
        jreleaserName = "osx-x86_64",
        taskSuffix = "OsxX64",
        jdkPath = "jdk/Contents/Home",
        archiveFormat = "tar.gz"
    ),
    WINDOWS_X64(
        temurinName = "x64_windows",
        jreleaserName = "windows-x86_64",
        taskSuffix = "WindowsX64",
        jdkPath = "jdk",
        archiveFormat = "zip"
    ),
    LINUX_X64(
        temurinName = "x64_linux",
        jreleaserName = "linux-x86_64",
        taskSuffix = "LinuxX64",
        jdkPath = "jdk",
        archiveFormat = "tar.gz"
    )
}

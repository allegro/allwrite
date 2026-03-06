package pl.allegro.tech.allwrite.runner.infrastructure.os.port.incoming

public interface SystemEnvironment {

    /**
     * Get value of env variable or system property (in this order)
     */
    public operator fun get(name: String): String?

    /**
     * Get all system envs or system properties
     */
    public fun getAll(): Map<String, String> = System.getenv()

    /**
     * Check whether given env variable or system property exists
     */
    public fun contains(name: String): Boolean =
        get(name) != null

    /**
     * Get value of env variable or system property or throw [IllegalStateException]
     */
    public fun require(name: String): String =
        get(name) ?: error("Environment variable '$name' is missing")

    /**
     * Try to get subsequent env variables or system properties until first non-null.
     *
     * Otherwise, throw [IllegalStateException]
     */
    public fun requireWithFallbacks(vararg names: String): String =
        if (names.size == 1) require(names.first())
        else names.firstNotNullOfOrNull(::get) ?: error("None of environment variables '$names' is defined")
}

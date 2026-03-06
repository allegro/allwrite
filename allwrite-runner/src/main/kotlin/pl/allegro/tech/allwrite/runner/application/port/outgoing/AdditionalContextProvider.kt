package pl.allegro.tech.allwrite.runner.application.port.outgoing

public interface AdditionalContextProvider {

    public fun extractFromSystemEnvs(): Map<String, String>
}

package pl.allegro.tech.allwrite.cli.application.port.outgoing

public interface ExternalRecipeStore {
    public fun list(): Map<String, String>
    public fun add(name: String, url: String)
    public fun update(name: String, url: String)
    public fun remove(name: String)
    public fun refresh(name: String)
}

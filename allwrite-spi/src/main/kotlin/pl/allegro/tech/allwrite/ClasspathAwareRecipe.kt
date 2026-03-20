package pl.allegro.tech.allwrite

public interface ClasspathAwareRecipe {

    public fun requireOnClasspath(): List<String>
}

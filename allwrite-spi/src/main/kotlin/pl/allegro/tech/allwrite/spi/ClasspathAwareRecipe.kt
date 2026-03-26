package pl.allegro.tech.allwrite.spi

public interface ClasspathAwareRecipe {

    public fun requireOnClasspath(): List<String>
}

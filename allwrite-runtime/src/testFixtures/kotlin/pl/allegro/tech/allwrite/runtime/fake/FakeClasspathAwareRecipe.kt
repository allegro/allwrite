package pl.allegro.tech.allwrite.runtime.fake

import pl.allegro.tech.allwrite.ClasspathAwareRecipe

class FakeClasspathAwareRecipe(
    private val classpath: List<String> = emptyList(),
) : FakeRecipe(),
    ClasspathAwareRecipe {

    override fun requireOnClasspath(): List<String> = classpath
}

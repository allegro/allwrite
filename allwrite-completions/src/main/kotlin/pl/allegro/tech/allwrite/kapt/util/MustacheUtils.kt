package pl.allegro.tech.allwrite.kapt.util

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.Mustache
import com.github.mustachejava.MustacheFactory
import com.github.mustachejava.reflect.ReflectionObjectHandler
import java.io.StringReader
import java.io.StringWriter

internal object MustacheUtils {
    private val factory: MustacheFactory

    init {
        factory = DefaultMustacheFactory()
        factory.objectHandler = object : ReflectionObjectHandler() {
            override fun areMethodsAccessible(map: MutableMap<*, *>?) = true
        }
    }

    fun resourceTemplate(name: String): Mustache? =
        this.javaClass.getResourceAsStream(name)?.let { res ->
            val template = res.bufferedReader().use { it.readText() }
            factory.compile(StringReader(template), name)
        }

    fun Mustache.executeToString(scope: Any): String {
        val writer = StringWriter()
        this.execute(writer, scope)
        return writer.toString()
    }
}

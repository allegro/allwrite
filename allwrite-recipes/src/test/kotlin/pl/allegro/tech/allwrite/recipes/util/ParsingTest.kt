package pl.allegro.tech.allwrite.recipes.util

import org.junit.jupiter.api.BeforeEach
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.kotlin.tree.K
import kotlin.jvm.optionals.getOrNull

abstract class ParsingTest {

    val javaParser: JavaParser = JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()).build()
    val kotlinParser: KotlinParser = KotlinParser.builder().classpath(JavaParser.runtimeClasspath()).build()
    val groovyParser: GroovyParser = GroovyParser.builder().classpath(JavaParser.runtimeClasspath()).build()

    @BeforeEach
    fun setup() {
        javaParser.reset()
        kotlinParser.reset()
        groovyParser.reset()
    }

    fun parseJava(java: String): J.CompilationUnit = javaParser.parse(java).findFirst().getOrNull() as J.CompilationUnit
    fun parseKotlin(kotlin: String): K.CompilationUnit = kotlinParser.parse(kotlin).findFirst().getOrNull() as K.CompilationUnit
    fun parseGroovy(groovy: String): G.CompilationUnit = groovyParser.parse(groovy).findFirst().getOrNull() as G.CompilationUnit
}

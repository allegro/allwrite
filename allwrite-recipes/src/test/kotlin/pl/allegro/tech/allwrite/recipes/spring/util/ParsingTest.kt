package pl.allegro.tech.allwrite.recipes.spring.util

import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.kotlin.tree.K
import pl.allegro.tech.allwrite.common.util.classpathFor
import kotlin.jvm.optionals.getOrNull

abstract class ParsingTest {
    val javaParser: JavaParser = JavaParser.fromJavaVersion()
        .classpath(classpathFor("spring-framework"))
        .build()
    val kotlinParser: KotlinParser = KotlinParser.builder()
        .classpath(classpathFor("spring-framework"))
        .build()

    fun parse(java: String): J.CompilationUnit = javaParser.parse(java).findFirst().getOrNull() as J.CompilationUnit
    fun parseKotlin(java: String): K.CompilationUnit = kotlinParser.parse(java).findFirst().getOrNull() as K.CompilationUnit
}
